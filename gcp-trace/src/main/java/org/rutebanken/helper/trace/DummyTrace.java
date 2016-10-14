package org.rutebanken.helper.trace;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.trace.DefaultTraceContextHandler;
import com.google.cloud.trace.ManagedTracer;
import com.google.cloud.trace.RawTracer;
import com.google.cloud.trace.TraceContextFactoryTracer;
import com.google.cloud.trace.TraceContextHandler;
import com.google.cloud.trace.TraceContextHandlerTracer;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.grpc.v1.GrpcTraceSink;
import com.google.cloud.trace.util.ConstantTraceOptionsFactory;
import com.google.cloud.trace.util.JavaTimestampFactory;
import com.google.cloud.trace.util.StackTrace;
import com.google.cloud.trace.util.ThrowableStackTraceHelper;
import com.google.cloud.trace.util.TimestampFactory;
import com.google.cloud.trace.util.TraceContextFactory;
import com.google.cloud.trace.v1.RawTracerV1;
import com.google.cloud.trace.v1.sink.TraceSink;
import com.google.cloud.trace.v1.source.TraceSource;

import java.io.IOException;
import java.util.logging.Logger;

public class DummyTrace {
    private final static Logger logger = Logger.getLogger(DummyTrace.class.getName());

    public void call() throws IOException {
        // Create the raw tracer.
        String projectId = System.getProperty("projectId", "carbon-1287");

        // Create the raw tracer.
        TraceSource traceSource = new TraceSource();
        TraceSink traceSink = new GrpcTraceSink("cloudtrace.googleapis.com",
                GoogleCredentials.getApplicationDefault());
        RawTracer rawTracer = new RawTracerV1(projectId, traceSource, traceSink);

        // Create the tracer.
        TraceContextFactory traceContextFactory = new TraceContextFactory(
                new ConstantTraceOptionsFactory(true, false));
        TimestampFactory timestampFactory = new JavaTimestampFactory();
        Tracer tracer = new TraceContextFactoryTracer(rawTracer, traceContextFactory, timestampFactory);

        // Create the managed tracer.
        TraceContextHandler traceContextHandler = new DefaultTraceContextHandler(
                traceContextFactory.rootContext());
        ManagedTracer managedTracer = new TraceContextHandlerTracer(tracer, traceContextHandler);

        // Create some trace data.
        managedTracer.startSpan("rutebanken-helper");

        managedTracer.startSpan("junit");

        StackTrace.Builder stackTraceBuilder = ThrowableStackTraceHelper.createBuilder(new Exception());
        managedTracer.setStackTrace(stackTraceBuilder.build());
        managedTracer.endSpan();

        managedTracer.endSpan();
    }
}