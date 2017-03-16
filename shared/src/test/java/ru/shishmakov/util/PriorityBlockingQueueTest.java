package ru.shishmakov.util;

import org.junit.Test;
import ru.shishmakov.BaseTest;
import ru.shishmakov.hz.TaskTime;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static junit.framework.TestCase.assertFalse;
import static org.apache.commons.lang3.ArrayUtils.isSorted;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmitriy Shishmakov on 16.03.17
 */
public class PriorityBlockingQueueTest extends BaseTest {

    @Test
    public void pollingShouldRetrieveSortedElements() {
        final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        final List<TaskTime> tasksBefore = LongStream.range(0, 10)
                .boxed()
                .map(id -> new TaskTime(id, now.plusHours(id), null))
                .unordered()
                .collect(Collectors.toList());

        Collections.shuffle(tasksBefore);
        assertFalse("Tasks should be unsorted", isSorted(tasksBefore.toArray(new TaskTime[tasksBefore.size()])));

        BlockingQueue<TaskTime> queue = new PriorityBlockingQueue<>(tasksBefore.size());
        tasksBefore.forEach(t -> QueueUtils.offer(queue, t));
        final List<TaskTime> tasksAfter = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) QueueUtils.poll(queue).ifPresent(tasksAfter::add);

        assertTrue("Tasks should be sorted", isSorted(tasksAfter.toArray(new TaskTime[tasksAfter.size()])));
    }
}
