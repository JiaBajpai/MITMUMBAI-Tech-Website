package com.mittechkernel.backend.modules.domain.service;

import com.mittechkernel.backend.common.exception.ResourceNotFoundException;
import com.mittechkernel.backend.modules.domain.dto.DomainResponse;
import com.mittechkernel.backend.modules.domain.entity.Domain;
import com.mittechkernel.backend.modules.domain.repository.DomainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DomainService {

    private final DomainRepository domainRepository;

    public DomainService(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    @Transactional(readOnly = true)
    public List<DomainResponse> getAllDomains() {
        return domainRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(DomainResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DomainResponse getDomain(Long id) {
        Domain domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domain", id));
        return DomainResponse.from(domain);
    }
}
