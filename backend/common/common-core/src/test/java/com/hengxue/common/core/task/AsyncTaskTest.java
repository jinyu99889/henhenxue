package com.hengxue.common.core.task;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsyncTaskTest {

    @Test
    void createsAPendingAcceptedTask() {
        TaskAccepted task = TaskAccepted.pending("01J00000000000000000000000", "LEARNING_TREE", "01J00000000000000000000001");

        assertEquals(TaskStatus.PENDING, task.status());
    }

    @Test
    void rejectsNonPendingAcceptedTasks() {
        assertThrows(IllegalArgumentException.class, () -> new TaskAccepted(
                "01J00000000000000000000000",
                TaskStatus.RUNNING,
                "LEARNING_TREE",
                "01J00000000000000000000001"
        ));
    }

    @Test
    void rejectsTaskProgressOutsideTheContractRange() {
        assertThrows(IllegalArgumentException.class, () -> new AsyncTask(
                "01J00000000000000000000000",
                TaskType.LEARNING_TREE_GENERATE,
                TaskStatus.RUNNING,
                101,
                "LEARNING_TREE",
                "01J00000000000000000000001",
                null,
                null,
                Instant.now(),
                null
        ));
        assertThrows(IllegalArgumentException.class, () -> taskWithProgress(-1));
    }

    @Test
    void acceptsTheProgressBoundaries() {
        assertEquals(0, taskWithProgress(0).progress());
        assertEquals(100, taskWithProgress(100).progress());
    }

    @Test
    void rejectsBlankTaskResourceFields() {
        assertEquals("任务 ID不能为空或空白", assertThrows(
                IllegalArgumentException.class,
                () -> TaskAccepted.pending(" ", "LEARNING_TREE", "01J00000000000000000000001")
        ).getMessage());
        assertEquals("资源类型不能为空或空白", assertThrows(
                IllegalArgumentException.class,
                () -> taskWithResourceType("")
        ).getMessage());
        assertEquals("资源 ID不能为空或空白", assertThrows(
                IllegalArgumentException.class,
                () -> new TaskAccepted("01J00000000000000000000000", TaskStatus.PENDING, "LEARNING_TREE", null)
        ).getMessage());
    }

    private AsyncTask taskWithProgress(int progress) {
        return new AsyncTask(
                "01J00000000000000000000000",
                TaskType.LEARNING_TREE_GENERATE,
                TaskStatus.RUNNING,
                progress,
                "LEARNING_TREE",
                "01J00000000000000000000001",
                null,
                null,
                Instant.now(),
                null
        );
    }

    private AsyncTask taskWithResourceType(String resourceType) {
        return new AsyncTask(
                "01J00000000000000000000000",
                TaskType.LEARNING_TREE_GENERATE,
                TaskStatus.RUNNING,
                0,
                resourceType,
                "01J00000000000000000000001",
                null,
                null,
                null,
                null
        );
    }
}
