package com.carbonplayer.model.network;

import com.carbonplayer.model.network.entity.DownloadRequest;
import com.carbonplayer.model.network.entity.InputStreamEntity;
import com.carbonplayer.model.network.entity.StreamingContent;
import com.carbonplayer.model.network.utils.TailInputStream;
import com.facebook.stetho.server.http.HttpHeaders;
import com.facebook.stetho.server.http.HttpStatus;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.util.HashMap;
import rx.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class StreamRequestHandler implements HttpRequestHandler {

    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=(\\d+)-?(\\d*)");
    private volatile DownloadRequest downloadRequest;
    private HashMap<String, StreamingContent> streams;
    private StreamingContent streamingContent;


    StreamRequestHandler(HashMap<String, StreamingContent> streams, Observable<StreamingContent> newStreams){
        this.streams = streams;
        newStreams.subscribe(s -> streams.put(s.getId().getId(), s));
    }

    public DownloadRequest getDownloadRequest(){
        return downloadRequest;
    }

    public void handle(HttpRequest request, HttpResponse response, HttpContext context) {
        RequestLine reqLine = request.getRequestLine();
        String path = reqLine.getUri();
        Timber.d("handle: " + reqLine);
        for (Header header : request.getAllHeaders()) {
            Timber.d("Header: %s: %s", header.getName(), header.getValue());
        }
        int lastSlash = path.lastIndexOf(47);
        if (lastSlash == -1) {
            throw new IllegalArgumentException("Unknown URL requested: " + path);
        }
        try {
            streamingContent = streams.get(path.substring(lastSlash + 1, path.length()));
            if (streamingContent == null) {
                Timber.wtf("Requesting file which is not allowed to be streamed: %s", this.streams);
                response.setStatusCode(HttpStatus.HTTP_NOT_FOUND);
                return;
            }
            Header headerRange = request.getLastHeader("Range");
            boolean usingRangeHeader = false;
            long startRangeByte = 0;
            if (headerRange != null) {
                Matcher matcher = RANGE_PATTERN.matcher(headerRange.getValue());
                if (matcher.matches()) {
                    startRangeByte = Long.parseLong(matcher.group(1));
                    Timber.i("Server requesting byte: %f", startRangeByte);
                    usingRangeHeader = true;
                }
            }
            String contentType = null;
            try {
                streamingContent.initialize(null);
                contentType = streamingContent.getContentType();
            } catch (InterruptedException e) {
                Timber.e("Failed to retrieve content type");
            }
            startRangeByte += streamingContent.getStartReadPoint();
            if (contentType == null) {
                Timber.e("Missing content type - exiting content=%s", streamingContent);
                response.setStatusCode(HttpStatus.HTTP_NOT_FOUND);
                return;
            }
            Timber.d("The content type is: %s", contentType);

            response.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
            response.addHeader("X-SocketTimeout", "60");
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.setEntity(new InputStreamEntity(streamingContent.toString(), new TailInputStream(streamingContent.getContext(), streamingContent, startRangeByte)));
            if (usingRangeHeader) {
                response.setStatusCode(206);
            } else {
                response.setStatusCode(HttpStatus.HTTP_OK);
            }
        } catch (NumberFormatException e2) {
            throw new IllegalArgumentException("Unknown URL requested: " + path);
        }
    }
}
