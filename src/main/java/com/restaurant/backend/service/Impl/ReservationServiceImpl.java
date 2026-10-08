package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.ReservationRequest;
import com.restaurant.backend.entity.Customer;
import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.Reservation;
import com.restaurant.backend.entity.ReservationStatus;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class ReservationServiceImpl implements ReservationService {

    // Mỗi lượt đặt giữ bàn tối đa 2 tiếng (có thể chỉnh)
    private static final long HOLD_HOURS = 2;
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
        validateBooking(request, table, null);

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
        validateBooking(request, table, id);

        existing.setCustomer(customer);
        existing.setTable(table);
        existing.setReservedAt(request.reservedAt());
        existing.setGuestCount(request.guestCount());
        return reservationRepository.save(existing);
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
        return reservationRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        reservationRepository.delete(getById(id));
    }

    // ---------- helpers ----------

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

    private void validateBooking(ReservationRequest r, DiningTable table, Long ignoreId) {
        if (r.reservedAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Thời gian đặt bàn phải ở tương lai");
        }
        if (r.guestCount() > table.getCapacity()) {
            throw new BadRequestException("Số khách vượt quá sức chứa của bàn (" + table.getCapacity() + ")");
        }
        LocalDateTime from = r.reservedAt().minusHours(HOLD_HOURS);
        LocalDateTime to = r.reservedAt().plusHours(HOLD_HOURS);
        boolean conflict = reservationRepository.findByTableId(table.getId()).stream()
                .filter(x -> ignoreId == null || !x.getId().equals(ignoreId))
                .filter(x -> ACTIVE.contains(x.getStatus()))
                .anyMatch(x -> x.getReservedAt().isAfter(from) && x.getReservedAt().isBefore(to));
        if (conflict) {
            throw new BadRequestException("Bàn đã có người đặt trong khung giờ này");
        }
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng id = " + id));
    }

    private DiningTable findTable(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn id = " + id));
    }
}
