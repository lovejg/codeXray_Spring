package com.codeXray.backend.note.repository;

import com.codeXray.backend.note.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long>, NoteRepositoryCustom {
}
