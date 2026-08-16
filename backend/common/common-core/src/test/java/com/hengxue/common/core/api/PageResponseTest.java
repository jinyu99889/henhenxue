package com.hengxue.common.core.api;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResponseTest {

    @Test
    void usesTheDocumentedDefaultPage() {
        assertEquals(new PageRequest(1, 20), PageRequest.firstPage());
    }

    @Test
    void rejectsAnOutOfRangePageSize() {
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(1, 101));
    }

    @Test
    void snapshotsItemsBeforeReturningThePage() {
        List<String> source = new ArrayList<>(List.of("first"));
        PageResponse<String> page = PageResponse.of(source, PageRequest.firstPage(), 1);
        source.add("second");

        assertEquals(List.of("first"), page.items());
        assertThrows(UnsupportedOperationException.class, () -> page.items().add("third"));
    }

    @Test
    void rejectsMissingItemsAndNegativeTotals() {
        assertEquals("当前页数据不能为空", assertThrows(
                NullPointerException.class,
                () -> new PageResponse<String>(null, 1, 20, 0)
        ).getMessage());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new PageResponse<>(List.of(), 1, 20, -1)
        );

        assertEquals("记录总数不能为负数", exception.getMessage());
    }

    @Test
    void rejectsAMissingPageRequestWhenUsingTheFactory() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> PageResponse.of(List.of("first"), null, 1)
        );

        assertTrue(exception.getMessage().contains("分页请求不能为空"));
    }
}
