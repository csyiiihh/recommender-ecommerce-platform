package org.example.springboot;

import org.example.springboot.controller.Dict.SysDictController;
import org.example.springboot.entity.Dict.SysDict;
import org.example.springboot.mapper.Dict.DictTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DictionaryTypeCreationTest {
    DictTypeMapper mapper;
    SysDictController controller;
    @BeforeEach void setup() {
        mapper = mock(DictTypeMapper.class); controller = new SysDictController();
        ReflectionTestUtils.setField(controller, "sysDictMapper", mapper);
    }
    SysDict value(String code) { SysDict d = new SysDict(); d.setDictTypeCode(code); return d; }
    @Test void emptyQueryAllowsCreatingNewType() {
        when(mapper.selectObjs(any())).thenReturn(List.of());
        when(mapper.insert(any(SysDict.class))).thenReturn(1);
        SysDict d = value("  genre  ");
        assertEquals("0", controller.save(d).getCode()); assertEquals("genre", d.getDictTypeCode());
        verify(mapper).insert(d);
    }
    @Test void existingCodeIsRejectedWithoutInsert() {
        when(mapper.selectObjs(any())).thenReturn(List.of("genre"));
        assertNotEquals("0", controller.save(value("genre")).getCode());
        verify(mapper, never()).insert(any(SysDict.class));
    }
    @Test void databaseUniqueConstraintCatchesConcurrentCreation() {
        when(mapper.selectObjs(any())).thenReturn(List.of());
        when(mapper.insert(any(SysDict.class))).thenThrow(new DuplicateKeyException("unique code"));
        var result = controller.save(value("genre"));
        assertNotEquals("0", result.getCode()); assertEquals("该类型已存在", result.getMsg());
    }
    @Test void failedInsertDoesNotReturnSuccess() {
        when(mapper.selectObjs(any())).thenReturn(List.of());
        assertNotEquals("0", controller.save(value("genre")).getCode());
    }
    @ParameterizedTest @NullSource @ValueSource(strings = {"", "   "})
    void blankCodeNeverTouchesDatabase(String code) {
        assertNotEquals("0", controller.save(value(code)).getCode()); verifyNoInteractions(mapper);
    }
}
