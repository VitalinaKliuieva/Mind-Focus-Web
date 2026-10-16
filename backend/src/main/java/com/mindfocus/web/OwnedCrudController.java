package com.mindfocus.web;

import com.mindfocus.domain.OwnedEntity;
import com.mindfocus.repo.OwnedRepository;
import com.mindfocus.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Generic list/create/update/delete for per-user entities.
 * Every query is scoped by the logged-in user id, so users can never touch each other's data.
 */
public abstract class OwnedCrudController<T extends OwnedEntity> {
    protected final OwnedRepository<T> repo;

    protected OwnedCrudController(OwnedRepository<T> repo) {
        this.repo = repo;
    }

    protected Sort sort() {
        return Sort.by(Sort.Direction.DESC, "id");
    }

    /** Copy the editable fields from the request body into the stored entity. */
    protected abstract void copyInto(T target, T source);

    /** Hook that runs before create/update is saved. */
    protected void prepare(T entity) {}

    /** Hook that runs before an entity is deleted. */
    protected void beforeDelete(T entity) {}

    @GetMapping
    public List<T> list() {
        return repo.findByUserId(CurrentUser.id(), sort());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public T create(@Valid @RequestBody T body) {
        body.setId(null);
        body.setUserId(CurrentUser.id());
        prepare(body);
        return repo.save(body);
    }

    @PutMapping("/{id}")
    @Transactional
    public T update(@PathVariable Long id, @Valid @RequestBody T body) {
        T entity = find(id);
        copyInto(entity, body);
        prepare(entity);
        return repo.save(entity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        T entity = find(id);
        beforeDelete(entity);
        repo.delete(entity);
    }

    protected T find(Long id) {
        return repo.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    }
}
