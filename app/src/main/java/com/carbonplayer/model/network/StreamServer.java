package com.carbonplayer.model.network;

import android.app.FragmentTransaction;
import android.content.Context;

import com.carbonplayer.model.network.entity.DownloadRequest;
import com.carbonplayer.model.network.entity.StreamingContent;
import com.google.common.base.Preconditions;

import org.apache.http.ConnectionClosedException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import rx.subjects.PublishSubject;
import timber.log.Timber;

//noinspection deprecation
public final class StreamServer {

    private Context context;
    private boolean shutdown = false;
    private ServerSocket serverSocket;
    private final LinkedList<WorkerThread> workers = new LinkedList<>();
    private HttpParams params;
    private RequestAcceptorThread acceptor;
    private HashMap<String, StreamingContent> streams;
    private PublishSubject<StreamingContent> newContent = PublishSubject.create();

    public StreamServer(Context context) throws IOException {
        this.context = context;
        this.params = new BasicHttpParams();
        this.params
                .setBooleanParameter("http.connection.stalecheck", false)
                .setBooleanParameter("http.tcp.nodelay", true)
                .setIntParameter("http.socket.timeout", 10000)
                .setIntParameter("http.socket.buffer-size", 0x2000)
                .setIntParameter("http.socket.linger", 2);
        bind(0);
    }

    public StreamServer(Context context, HashMap<String, StreamingContent> streams) throws IOException {
        this(context);
        if(streams == null) Timber.e("Streams are null");
        this.streams = streams;
    }

    private void bind(int port) throws IOException {
        bind(new InetSocketAddress(InetAddress.getByAddress("localhost", new byte[]{Byte.MAX_VALUE, (byte) 0, (byte) 0, (byte) 1}), port));
    }

    private String generateAndSetUri(StreamingContent streamingContent) {
        String id = streamingContent.getId().getId();
        if(streams == null) streams = new HashMap<>();
        if(!streams.containsKey(id)) {
            streams.put(id, streamingContent);
            newContent.onNext(streamingContent);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("http://127.0.0.1:").append(getPort()).append("/").append(streamingContent.getId().getId());
        streamingContent.setUrl(sb.toString());
        return sb.toString();
    }

    public String serveStream(StreamingContent streamingContent) {
        acceptor.shutdownOldWorkers();
        return generateAndSetUri(streamingContent);
    }

    private int getPort() {
        if (serverSocket != null) {
            return serverSocket.getLocalPort();
        }
        throw new IllegalStateException("Socket not bound");
    }

    private void bind(InetSocketAddress addr) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(addr);
        Timber.i("Bound to port: %d", serverSocket.getLocalPort());
        if (acceptor != null) {
            throw new RuntimeException("Should never bind to a socket twice");
        }
        acceptor = new RequestAcceptorThread();
        acceptor.start();
    }

    private class RequestAcceptorThread extends Thread {
        RequestAcceptorThread() {
            super("RequestAcceptorThread");
        }

        public void run() {
            while (!shutdown) {
                try {
                    Socket socket = serverSocket.accept();
                    synchronized (workers) {
                        WorkerThread worker = new WorkerThread(socket);
                        workers.add(worker);
                        worker.start();
                        if (workers.size() > 2) {
                            StringBuilder log = new StringBuilder();
                            log.append("More than 2 worker running: ");

                            for (WorkerThread w : workers) {
                                log.append(w.getDownloadRequest());
                            }
                            Timber.d(log.toString());
                        }
                    }
                } catch (IOException e) {
                    if (!shutdown) {
                        Timber.e("RequestAcceptorThread exited abnormally: " + e.getMessage(), e);
                        return;
                    }
                    return;
                }
            }
        }

        private void shutdownOldWorkers() {
            synchronized (workers) {
                for(WorkerThread worker: workers){
                    DownloadRequest workerDownloadRequest = worker.getDownloadRequest();
                    if (workerDownloadRequest != null) {
                        worker.shutdown();
                        workers.remove();
                    }
                }
            }
        }
    }

    private class WorkerThread extends Thread {
        private StreamRequestHandler handler;
        private final Socket socket;

        public WorkerThread(Socket socket) {
            super("StreamingHttpServer.WorkerThread");
            setDaemon(true);
            this.socket = socket;
        }

        public void run() {
            DefaultHttpServerConnection conn = null;
            this.handler = new StreamRequestHandler(streams, newContent);
            try {
                DefaultHttpServerConnection conn2 = new DefaultHttpServerConnection();
                try {
                    conn2.bind(socket, params);
                    BasicHttpProcessor processor = new BasicHttpProcessor();
                    processor.addInterceptor(new ResponseContent());
                    processor.addInterceptor(new ResponseConnControl());
                    HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
                    registry.register("*", this.handler);
                    HttpService service = new HttpService(processor, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
                    service.setParams(params);
                    service.setHandlerResolver(registry);
                    HttpContext httpContext = new BasicHttpContext(null);
                    Timber.d("WorkerThread: handling request for socket %s with download request %s, connection %s", this.socket, getDownloadRequest(), conn2);
                    service.handleRequest(conn2, httpContext);
                    closeConnection(conn2);
                    conn = conn2;
                } catch (Exception e) {
                    conn = conn2;
                    if (e instanceof ConnectionClosedException) {
                        Timber.e(e, "HTTP server disrupted: " + e.toString());
                    } else {
                        Timber.i("StreamServer.Worker connection closed");
                    }
                    closeConnection(conn);
                }
            } catch (Throwable t4) {
                Timber.i("StreamingHttpServer.Worker connection closed");

                closeConnection(conn);
            }
        }

        private void closeConnection(DefaultHttpServerConnection conn) {
            try {
                Timber.d("WorkerThread closing connection %s or socket %s", conn, socket);
                if (conn != null) {
                    conn.shutdown();
                } else if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
            }
            synchronized (workers) {
                workers.remove(this);
            }
        }

        public DownloadRequest getDownloadRequest() {
            if (this.handler != null) {
                return this.handler.getDownloadRequest();
            }
            return null;
        }

        public void shutdown() {
            Timber.d("Worker.shutdown() for request: %s", getDownloadRequest());
            interrupt();
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }
}
