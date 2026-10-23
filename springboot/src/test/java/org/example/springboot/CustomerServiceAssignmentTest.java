package org.example.springboot;

import org.example.springboot.entity.CustomerServiceSession;
import org.example.springboot.mapper.CustomerServiceSessionMapper;
import org.example.springboot.service.CustomerServiceService;
import org.example.springboot.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceAssignmentTest {
    @Mock CustomerServiceSessionMapper sessionMapper;
    @InjectMocks CustomerServiceService service;
    @Test void anotherStaffMemberCannotTakeOverAnAssignedSession() {
        CustomerServiceSession session = new CustomerServiceSession(); session.setId(1L); session.setServiceId(2L); session.setStatus(1);
        when(sessionMapper.selectById(1L)).thenReturn(session);
        assertThrows(BusinessException.class, () -> service.acceptSession(1L, 3L));
        verify(sessionMapper, never()).updateById(any(CustomerServiceSession.class));
        verify(sessionMapper, never()).update(any(), any());
    }
    @Test void closedSessionCannotBeReopenedThroughAccept() {
        CustomerServiceSession session = new CustomerServiceSession(); session.setId(1L); session.setStatus(2);
        when(sessionMapper.selectById(1L)).thenReturn(session);
        assertThrows(BusinessException.class, () -> service.acceptSession(1L, 3L));
        verify(sessionMapper, never()).updateById(any(CustomerServiceSession.class));
        verify(sessionMapper, never()).update(any(), any());
    }
}
