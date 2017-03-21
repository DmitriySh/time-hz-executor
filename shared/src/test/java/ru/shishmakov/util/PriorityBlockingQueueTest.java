package ru.shishmakov.util;

import org.junit.Test;
import ru.shishmakov.BaseTest;
import ru.shishmakov.hz.TimeTask;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
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

    private static final Callable<Void> DUMMY_TASK = () -> null;

    @Test
    public void pollingShouldRetrieveSortedElements() {
        final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        final List<TimeTask> tasksBefore = LongStream.range(0, 10)
                .boxed().map(id -> new TimeTask(id, now.plusHours(id), DUMMY_TASK))
                .collect(Collectors.toList());

        Collections.shuffle(tasksBefore);
        assertFalse("Tasks should be unsorted", isSorted(tasksBefore.toArray(new TimeTask[tasksBefore.size()])));

        BlockingQueue<TimeTask> queue = new PriorityBlockingQueue<>(tasksBefore.size());
        tasksBefore.forEach(t -> QueueUtils.offer(queue, t));
        final List<TimeTask> tasksAfter = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) QueueUtils.poll(queue).ifPresent(tasksAfter::add);

        assertTrue("Tasks should be sorted", isSorted(tasksAfter.toArray(new TimeTask[tasksAfter.size()])));
    }
}
