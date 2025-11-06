package com.lumeo.stream.lumeo.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lumeo.stream.lumeo.entity.Video;

@Repository
public interface VideoRepo extends JpaRepository <Video,Long> {

    
    
}
