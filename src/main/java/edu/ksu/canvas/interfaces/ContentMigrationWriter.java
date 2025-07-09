package edu.ksu.canvas.interfaces;

import edu.ksu.canvas.model.ContentMigration;
import edu.ksu.canvas.requestOptions.CreateContentMigrationOptions;
import edu.ksu.canvas.requestOptions.CreateCourseContentMigrationOptions;
import org.apache.hc.core5.http.ProtocolException;

import java.io.IOException;

import java.net.URISyntaxException;
import java.util.Optional;

public interface ContentMigrationWriter extends CanvasWriter<ContentMigration, ContentMigrationWriter>{

    Optional<ContentMigration> createCourseContentMigration(CreateCourseContentMigrationOptions options) throws IOException, ProtocolException, URISyntaxException;
    Optional<ContentMigration> updateCourseContentMigration(Long id, CreateCourseContentMigrationOptions options) throws IOException, ProtocolException, URISyntaxException;

    Optional<ContentMigration> createUserContentMigration(CreateContentMigrationOptions options) throws IOException, ProtocolException, URISyntaxException;
    Optional<ContentMigration> updateUserContentMigration(Long id, CreateContentMigrationOptions options) throws IOException, ProtocolException, URISyntaxException;

    Optional<ContentMigration> createGroupContentMigration(CreateContentMigrationOptions options) throws IOException, ProtocolException, URISyntaxException;
    Optional<ContentMigration> updateGroupContentMigration(Long id, CreateContentMigrationOptions options) throws IOException, ProtocolException, URISyntaxException;

    Optional<ContentMigration> createAccountContentMigration(CreateContentMigrationOptions options) throws IOException, ProtocolException, URISyntaxException;
    Optional<ContentMigration> updateAccountContentMigration(Long id, CreateContentMigrationOptions options) throws IOException, ProtocolException, URISyntaxException;
}
