package edu.ksu.canvas.interfaces;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import edu.ksu.canvas.model.CommunicationChannel;
import edu.ksu.canvas.requestOptions.CreateCommunicationChannelOptions;
import org.apache.hc.core5.http.ProtocolException;

public interface CommunicationChannelWriter extends CanvasWriter<CommunicationChannel, CommunicationChannelWriter> {

    /**
     * Adds a communication channel to a Canvas user.
     * @param options The communication channel to create. Must contain user ID and address.
     * @return The created communication channel object if creation was successful
     * @throws IOException When there is an error communicating with Canvas
     */
    Optional<CommunicationChannel> createCommunicationChannel(CreateCommunicationChannelOptions options) throws IOException, ProtocolException, URISyntaxException;

    /**
     * Delete a communication channel from Canvas
     * @param cc The communication channel record to delete. Must contain User ID and communication channel ID.
     * @return The now deleted communication channel record
     * @throws IOException When there is an error communicating with Canvas
     */
    Optional<CommunicationChannel> deleteCommunicationChannel(CommunicationChannel cc) throws IOException, ProtocolException, URISyntaxException;
}
