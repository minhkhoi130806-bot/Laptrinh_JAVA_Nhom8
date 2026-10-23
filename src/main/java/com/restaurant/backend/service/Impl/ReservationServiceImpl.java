package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.ReservationRequest;
import com.restaurant.backend.entity.Customer;
import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.Reservation;
import com.restaurant.backend.entity.ReservationStatus;
import com.restaurant.backend.entity.TableStatus;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.mapper.ReservationMapper;
import com.restaurant.backend.repository.CustomerRepository;
import com.restaurant.backend.repository.DiningTableRepository;
import com.restaurant.backend.repository.ReservationRepository;
import com.restaurant.backend.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ReservationServiceImpl implements ReservationService {

    // Mỗi lượt đặt giữ bàn tối đa 2 tiếng (có thể chỉnh)
    private static final long HOLD_HOURS = 2;
    // Trước giờ đặt bao nhiêu phút thì bàn chuyển sang "Đã đặt" (và giữ thêm bấy nhiêu phút sau giờ hẹn)
    private static final long RESERVE_WINDOW_MINUTES = 30;
    private static final Set<ReservationStatus> ACTIVE =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final DiningTableRepository tableRepository;
    private final ReservationMapper reservationMapper;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  CustomerRepository customerRepository,
                                  DiningTableRepository tableRepository,
                                  ReservationMapper reservationMapper) {
        this.reservationRepository = reservationRepository;
        this.customerRepository = customerRepository;
        this.tableRepository = tableRepository;
        this.reservationMapper = reservationMapper;
    }

    @Override
    @Transactional
    public Reservation create(ReservationRequest request) {
        validateBasic(request);
        Customer customer = findCustomer(request.customerId());
        DiningTable table = findTable(request.tableId());
        validateBooking(request, table, null, true);
        Reservation reservation = reservationMapper.toEntity(request, customer, table);
        reservation.setId(null);
        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getAll(Long customerId, Long tableId) {
        if (customerId != null) {
            return reservationRepository.findByCustomerId(customerId);
        }
        if (tableId != null) {
            return reservationRepository.findByTableId(tableId);
        }
        return reservationRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lượt đặt bàn id = " + id));
    }

    @Override
    @Transactional
    public Reservation update(Long id, ReservationRequest request) {
        validateBasic(request);
        Reservation existing = getById(id);
        if (!ACTIVE.contains(existing.getStatus())) {
            throw new BadRequestException("Chỉ sửa được lượt đặt đang chờ hoặc đã xác nhận");
        }
        Customer customer = findCustomer(request.customerId());
        DiningTable table = findTable(request.tableId());
        DiningTable oldTable = existing.getTable();
        // Chỉ bắt buộc "giờ ở tương lai" khi người dùng đổi giờ (sửa số khách / ghi chú của lượt đã quá giờ vẫn được)
        boolean timeChanged = !Objects.equals(existing.getReservedAt(), request.reservedAt());
        validateBooking(request, table, id, timeChanged);

        existing.setCustomer(customer);
        existing.setTable(table);
        existing.setReservedAt(request.reservedAt());
        existing.setGuestCount(request.guestCount());
        existing.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        Reservation saved = reservationRepository.save(existing);

        // Đổi bàn: trả bàn cũ (nếu đang giữ chỗ) và giữ chỗ bàn mới nếu sắp đến giờ
        if (!oldTable.getId().equals(table.getId())) {
            releaseIfFree(oldTable);
        }
        if (saved.getStatus() == ReservationStatus.CONFIRMED) {
            reserveIfDue(saved);
        }
        return saved;
    }

    @Override
    @Transactional
    public Reservation updateStatus(Long id, ReservationStatus status) {
        if (status == null) {
            throw new BadRequestException("Trạng thái không được để trống");
        }
        Reservation existing = getById(id);
        ReservationStatus current = existing.getStatus();
        if (current == ReservationStatus.CANCELLED || current == ReservationStatus.COMPLETED) {
            throw new BadRequestException("Lượt đặt đã kết thúc, không thể đổi trạng thái");
        }
        existing.setStatus(status);
        Reservation saved = reservationRepository.save(existing);

        // Đồng bộ trạng thái bàn
        if (status == ReservationStatus.CONFIRMED) {
            reserveIfDue(saved);
        } else if (status == ReservationStatus.CANCELLED || status == ReservationStatus.COMPLETED) {
            releaseIfFree(saved.getTable());
        }
        return saved;
    }

    @Override
    @Transactional
    public Reservation checkIn(Long id) {
        Reservation existing = getById(id);
        if (!ACTIVE.contains(existing.getStatus())) {
            throw new BadRequestException("Lượt đặt đã kết thúc, không thể nhận bàn");
        }
        DiningTable table = existing.getTable();
        if (Boolean.TRUE.equals(table.getDeleted())) {
            throw new BadRequestException("Bàn này đã bị xóa");
        }
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new BadRequestException("Bàn " + table.getTableCode() + " đang có khách, hãy trả bàn hoặc đổi sang bàn khác trước");
        }
        existing.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(existing);
        table.setStatus(TableStatus.OCCUPIED);
        tableRepository.save(table);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Reservation existing = getById(id);
        DiningTable table = existing.getTable();
        reservationRepository.delete(existing);
        releaseIfFree(table);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiningTable> findAvailableTables(LocalDateTime at, Integer guests, Long ignoreReservationId) {
        if (at == null) {
            throw new BadRequestException("Thiếu thời gian đặt bàn");
        }
        return tableRepository.findByDeleted(false).stream()
                .filter(t -> guests == null || t.getCapacity() >= guests)
                .filter(t -> !hasConflict(t.getId(), at, ignoreReservationId))
                .sorted(Comparator.comparing((DiningTable t) -> t.getCapacity())
        .thenComparing(t -> t.getTableCode()))
                .toList();
    }

    @Override
    @Transactional
    public void syncTableStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> due = reservationRepository.findByStatusAndReservedAtBetween(
                ReservationStatus.CONFIRMED,
                now.minusMinutes(RESERVE_WINDOW_MINUTES),
                now.plusMinutes(RESERVE_WINDOW_MINUTES));
        for (Reservation r : due) {
            DiningTable t = r.getTable();
            if (!Boolean.TRUE.equals(t.getDeleted()) && t.getStatus() == TableStatus.AVAILABLE) {
                t.setStatus(TableStatus.RESERVED);
                tableRepository.save(t);
            }
        }
    }

    // ---------- helpers ----------

    // Lượt đã xác nhận mà đã sát giờ thì giữ chỗ ngay (bàn đang trống -> "Đã đặt")
    private void reserveIfDue(Reservation r) {
        LocalDateTime now = LocalDateTime.now();
        boolean due = !r.getReservedAt().isBefore(now.minusMinutes(RESERVE_WINDOW_MINUTES))
                && !r.getReservedAt().isAfter(now.plusMinutes(RESERVE_WINDOW_MINUTES));
        DiningTable t = r.getTable();
        if (due && t.getStatus() == TableStatus.AVAILABLE) {
            t.setStatus(TableStatus.RESERVED);
            tableRepository.save(t);
        }
    }

    // Bàn đang "Đã đặt" mà không còn lượt xác nhận nào sát giờ -> trả về "Trống"
    private void releaseIfFree(DiningTable table) {
        if (table.getStatus() != TableStatus.RESERVED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean stillHeld = !reservationRepository.findByTableIdAndStatusAndReservedAtBetween(
                table.getId(), ReservationStatus.CONFIRMED,
                now.minusMinutes(RESERVE_WINDOW_MINUTES), now.plusMinutes(RESERVE_WINDOW_MINUTES)).isEmpty();
        if (!stillHeld) {
            table.setStatus(TableStatus.AVAILABLE);
            tableRepository.save(table);
        }
    }

    private void validateBasic(ReservationRequest r) {
        if (r == null || r.customerId() == null) {
            throw new BadRequestException("Thiếu khách hàng");
        }
        if (r.tableId() == null) {
            throw new BadRequestException("Thiếu bàn");
        }
        if (r.reservedAt() == null) {
            throw new BadRequestException("Thiếu thời gian đặt bàn");
        }
        if (r.guestCount() == null || r.guestCount() <= 0) {
            throw new BadRequestException("Số khách phải lớn hơn 0");
        }
    }

    private void validateBooking(ReservationRequest r, DiningTable table, Long ignoreId, boolean requireFuture) {
        if (requireFuture && r.reservedAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Thời gian đặt bàn phải ở tương lai");
        }
        if (r.guestCount() > table.getCapacity()) {
            throw new BadRequestException("Số khách vượt quá sức chứa của bàn (" + table.getCapacity() + ")");
        }
        if (hasConflict(table.getId(), r.reservedAt(), ignoreId)) {
            throw new BadRequestException("Bàn đã có người đặt trong khung giờ này");
        }
    }

    // Có lượt đặt khác (đang chờ / đã xác nhận) cách giờ này dưới HOLD_HOURS không
    private boolean hasConflict(Long tableId, LocalDateTime at, Long ignoreId) {
        LocalDateTime from = at.minusHours(HOLD_HOURS);
        LocalDateTime to = at.plusHours(HOLD_HOURS);
        return reservationRepository.findByTableId(tableId).stream()
                .filter(x -> ignoreId == null || !x.getId().equals(ignoreId))
                .filter(x -> ACTIVE.contains(x.getStatus()))
                .anyMatch(x -> x.getReservedAt().isAfter(from) && x.getReservedAt().isBefore(to));
    }

    private Customer findCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng id = " + id));
        if (Boolean.TRUE.equals(customer.getDeleted())) {
            throw new BadRequestException("Khách hàng này đã bị xóa");
        }
        return customer;
    }

    private DiningTable findTable(Long id) {
        DiningTable table = tableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn id = " + id));
        if (Boolean.TRUE.equals(table.getDeleted())) {
            throw new BadRequestException("Bàn này đã bị xóa");
        }
        return table;
    }
}
