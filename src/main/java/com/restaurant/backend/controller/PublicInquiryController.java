package com.restaurant.backend.controller;

import com.restaurant.backend.dto.ContactMessageRequest;
import com.restaurant.backend.dto.EventInquiryRequest;
import com.restaurant.backend.entity.ContactMessage;
import com.restaurant.backend.entity.EventInquiry;
import com.restaurant.backend.repository.ContactMessageRepository;
import com.restaurant.backend.repository.EventInquiryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** API công khai lưu liên hệ và yêu cầu tư vấn tiệc vào MySQL. */
@RestController
@RequestMapping("/api")
public class PublicInquiryController {
    private final ContactMessageRepository contactRepository;
    private final EventInquiryRepository eventRepository;

    public PublicInquiryController(ContactMessageRepository contactRepository, EventInquiryRepository eventRepository) {
        this.contactRepository = contactRepository;
        this.eventRepository = eventRepository;
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createContact(@Valid @RequestBody ContactMessageRequest request) {
        ContactMessage item = new ContactMessage();
        item.setFullName(request.name().trim());
        item.setEmail(request.email() == null ? "" : request.email().trim());
        item.setPhone(request.phone() == null ? null : request.phone().trim());
        item.setSubject(request.subject().trim());
        item.setMessage(request.message().trim());
        item.setStatus("NEW");
        item = contactRepository.save(item);
        return Map.of("id", item.getId(), "status", item.getStatus(), "message", "Đã tiếp nhận lời nhắn.");
    }

    @PostMapping("/event-inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createEventInquiry(@Valid @RequestBody EventInquiryRequest request) {
        EventInquiry item = new EventInquiry();
        item.setContactName(request.name().trim());
        item.setPhone(request.phone().trim());
        item.setEmail(request.email() == null || request.email().isBlank() ? null : request.email().trim());
        item.setEventType(request.event().trim());
        item.setEventAt(request.date() == null ? null : request.date().atStartOfDay());
        item.setGuestCount(request.guests());
        item.setBudget(request.budget());
        item.setNotes(request.note() == null ? null : request.note().trim());
        item.setStatus("NEW");
        item = eventRepository.save(item);
        return Map.of("id", item.getId(), "status", item.getStatus(), "message", "Đã tiếp nhận yêu cầu tư vấn tiệc.");
    }
}
