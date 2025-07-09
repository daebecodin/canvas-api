package edu.ksu.canvas.interfaces;

import edu.ksu.canvas.model.FeatureFlag;
import org.apache.hc.core5.http.ProtocolException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

public interface FeatureFlagWriter extends CanvasWriter<FeatureFlag, FeatureFlagWriter > {

    Optional<FeatureFlag> updateCourseFeatureFlag(String courseId, String feature, FeatureFlag.State state) throws IOException, ProtocolException, URISyntaxException;
    Optional<FeatureFlag> updateAccountFeatureFlag(String accountId, String feature, FeatureFlag.State state) throws IOException, ProtocolException, URISyntaxException;
    Optional<FeatureFlag> updateUserFeatureFlag(String userId, String feature, FeatureFlag.State state) throws IOException, ProtocolException, URISyntaxException;

}
