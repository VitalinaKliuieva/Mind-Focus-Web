package com.mindfocus.web;

import com.mindfocus.domain.TodoItem;
import com.mindfocus.repo.TodoRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/todos")
public class TodoController extends OwnedCrudController<TodoItem> {
    public TodoController(TodoRepository repo) {
        super(repo);
    }

    @Override
    protected void copyInto(TodoItem t, TodoItem s) {
        t.setTitle(s.getTitle());
        t.setDueDate(s.getDueDate());
        if (t.isDone() != s.isDone()) {
            t.setDone(s.isDone());
            t.setCompletedAt(s.isDone() ? LocalDateTime.now() : null);
        }
    }

    @Override
    protected void prepare(TodoItem t) {
        if (t.isDone() && t.getCompletedAt() == null) {
            t.setCompletedAt(LocalDateTime.now());
        }
    }
}
