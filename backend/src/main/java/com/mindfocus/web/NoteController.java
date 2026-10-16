package com.dayfolio.web;

import com.dayfolio.domain.Note;
import com.dayfolio.repo.NoteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class NoteController extends OwnedCrudController<Note> {
    public NoteController(NoteRepository repo) {
        super(repo);
    }

    @Override
    protected Sort sort() {
        return Sort.by(Sort.Direction.DESC, "noteDate", "id");
    }

    @Override
    protected void copyInto(Note t, Note s) {
        t.setTitle(s.getTitle());
        t.setContent(s.getContent());
        t.setNoteDate(s.getNoteDate());
    }
}
