package org.example.dataprovider.repository;

import org.example.dataprovider.entity.Film;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.webmvc.RepositoryRestController;

import java.util.List;

@RepositoryRestController
public interface FilmRepository extends JpaRepository<Film, Long> {

    List<Film> findAllByGenreId(Long genreId);

}
