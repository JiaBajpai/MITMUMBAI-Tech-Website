package com.mittechkernel.backend.modules.admin.dto;

import java.util.List;

public record AccountResponse(long id, String email, String name, String program, boolean active,
                              List<String> roles, List<Long> domainIds, boolean passwordSetupRequired) {
}
