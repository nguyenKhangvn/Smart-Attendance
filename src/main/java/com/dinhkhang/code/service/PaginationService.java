package com.dinhkhang.code.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PaginationService {

    public Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }

        return PageRequest.of(page, size, sort);
    }

    public Pageable createPageable(int page, int size) {
        return PageRequest.of(page, size);
    }

    public int getTotalPages(long totalElements, int size) {
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNextPage(int currentPage, int totalPages) {
        return currentPage < totalPages - 1;
    }

    public boolean hasPreviousPage(int currentPage) {
        return currentPage > 0;
    }
}