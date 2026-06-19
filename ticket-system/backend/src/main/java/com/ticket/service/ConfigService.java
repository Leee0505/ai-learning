package com.ticket.service;

import com.ticket.dto.request.*;
import com.ticket.dto.response.*;

import java.util.List;

public interface ConfigService {
    // Field config
    List<FieldConfigResponse> listFields();
    FieldConfigResponse createField(CreateFieldRequest request, Long adminId);
    FieldConfigResponse updateField(Long id, UpdateFieldRequest request, Long adminId);
    void deleteField(Long id);
    void reorderFields(ReorderFieldsRequest request);

    // SLA config
    List<SlaConfigResponse> listSla();
    SlaConfigResponse updateSla(Long id, UpdateSlaRequest request, Long adminId);
}
