package com.hengxue.common.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageRequestTest {

    @Test
    void rejectsAPageBeforeTheFirstPage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, 20));

        assertEquals("页码必须大于或等于 1", exception.getMessage());
    }

    @Test
    void rejectsPageSizesOutsideTheContractRange() {
        assertEquals("每页数量必须在 1 至 100 之间", assertThrows(
                IllegalArgumentException.class,
                () -> new PageRequest(1, 0)
        ).getMessage());
        assertEquals("每页数量必须在 1 至 100 之间", assertThrows(
                IllegalArgumentException.class,
                () -> new PageRequest(1, 101)
        ).getMessage());
    }

    @Test
    void acceptsTheUpperPageSizeBoundary() {
        assertEquals(new PageRequest(1, PageRequest.MAX_PAGE_SIZE), new PageRequest(1, 100));
    }
}
