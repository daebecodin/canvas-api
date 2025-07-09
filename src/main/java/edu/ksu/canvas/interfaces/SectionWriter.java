package edu.ksu.canvas.interfaces;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import edu.ksu.canvas.model.Section;
import org.apache.hc.core5.http.ProtocolException;

/**
 * Created by japshvincent on 4/28/16.
 */
public interface SectionWriter extends CanvasWriter<Section, SectionWriter> {

    /**
     * Create a new section in Canvas.
     *
     * @param courseId identifies the parent course we are adding this section to
     * @param section a section object containing the information needed to create
     *         the section in Canvas
     * @param enableSisReactivation when true, will first try to re-activate a
     *         deleted section with matching sis_section_id if possible. Ignored if null
     * @return the newly created section
     * @throws IOException When there is an error communicating with Canvas
     */
    Optional<Section> createSection(String courseId, Section section, Boolean enableSisReactivation) throws IOException;

    /**
     * Update a section in Canvas.
     *
     * @param section section object with changes made that will be persisted
     * @return the updated section
     * @throws IOException when there is an error communicating with Canvas
     */
    Optional<Section> updateSection(Section section) throws IOException;

    /**
     * Delete an existing section in Canvas.
     *
     * @param sectionId identifies the section to delete
     * @return the section object that was deleted
     * @throws IOException When there is an error communicating with Canvas
     */
    Optional<Section> deleteSection(String sectionId) throws IOException, ProtocolException, URISyntaxException;

    /**
     * Move the Section to another course in Canvas.
     *
     * @param sectionId identifies the section to move
     * @param courseId identifies the course to move the section into
     * @return the section object that was moved
     * @throws IOException When there is an error communicating with Canvas
     */
    Optional<Section> crosslist(String sectionId, String courseId) throws IOException;
}
