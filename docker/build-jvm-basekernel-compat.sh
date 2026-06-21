#!/usr/bin/env bash
# Rebuild jupyter-jvm-basekernel 2.3.0 with protocol 5.4 control-channel stubs.
# JupyterLab 4.5 / jupyter_server 2.14+ sends debug_request on the control channel;
# the upstream 2019 kernel logs "Unhandled message: none" and never completes handshake.
set -euo pipefail

VERSION="2.3.0-trevas"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "${WORKDIR}"' EXIT

curl -fsSL \
	"https://github.com/SpencerPark/jupyter-jvm-basekernel/archive/refs/tags/v2.3.0.tar.gz" \
	| tar xz -C "${WORKDIR}"
SRC="${WORKDIR}/jupyter-jvm-basekernel-2.3.0"

MSG="${SRC}/src/main/java/io/github/spencerpark/jupyter/messages/MessageType.java"
BASE="${SRC}/src/main/java/io/github/spencerpark/jupyter/kernel/BaseKernel.java"
REQ_DIR="${SRC}/src/main/java/io/github/spencerpark/jupyter/messages/request"
REP_DIR="${SRC}/src/main/java/io/github/spencerpark/jupyter/messages/reply"

python3 - <<'PY' "${MSG}"
from pathlib import Path
import sys
path = Path(sys.argv[1])
text = path.read_text()
needle = '    public static final MessageType<InterruptRequest> INTERRUPT_REQUEST = new MessageType<>("interrupt_request", InterruptRequest.class);\n'
insert = needle + (
    '\n'
    '    public static final MessageType<DebugRequest> DEBUG_REQUEST = new MessageType<>("debug_request", DebugRequest.class);\n'
    '    public static final MessageType<DebugReply> DEBUG_REPLY = new MessageType<>("debug_reply", DebugReply.class);\n'
)
if 'DEBUG_REQUEST' not in text:
    if needle not in text:
        raise SystemExit('MessageType.java patch anchor not found')
    path.write_text(text.replace(needle, insert, 1))
PY

python3 - <<'PY' "${BASE}"
from pathlib import Path
import sys
path = Path(sys.argv[1])
text = path.read_text()
import_line = 'import io.github.spencerpark.jupyter.messages.request.*;\n'
extra = 'import io.github.spencerpark.jupyter.messages.request.DebugRequest;\nimport io.github.spencerpark.jupyter.messages.reply.DebugReply;\n'
if 'DebugRequest' not in text:
    text = text.replace(import_line, import_line + extra, 1)
handler_line = '        connection.setHandler(MessageType.INTERRUPT_REQUEST, this::handleInterruptRequest);\n'
handler_insert = handler_line + (
    '        connection.setHandler(MessageType.DEBUG_REQUEST, this::handleDebugRequest);\n'
)
if 'handleDebugRequest' not in text:
    if handler_line not in text:
        raise SystemExit('BaseKernel.java handler anchor not found')
    text = text.replace(handler_line, handler_insert, 1)
method = '''
    private void handleDebugRequest(ShellReplyEnvironment env, Message<DebugRequest> debugRequestMessage) {
        env.setBusyDeferIdle();
        DebugRequest request = debugRequestMessage.getContent();
        env.reply(DebugReply.unsupported(request != null ? request.getSeq() : null));
    }

'''
anchor = '    private void handleInterruptRequest'
if 'private void handleDebugRequest' not in text:
    text = text.replace(anchor, method + anchor, 1)
path.write_text(text)
PY

cat >"${REQ_DIR}/DebugRequest.java" <<'EOF'
package io.github.spencerpark.jupyter.messages.request;

import com.google.gson.annotations.SerializedName;
import io.github.spencerpark.jupyter.messages.ContentType;
import io.github.spencerpark.jupyter.messages.MessageType;
import io.github.spencerpark.jupyter.messages.RequestType;
import io.github.spencerpark.jupyter.messages.reply.DebugReply;

public class DebugRequest implements ContentType<DebugRequest>, RequestType<DebugReply> {
    public static final MessageType<DebugRequest> MESSAGE_TYPE = MessageType.DEBUG_REQUEST;
    public static final MessageType<DebugReply> REPLY_MESSAGE_TYPE = MessageType.DEBUG_REPLY;

    @SerializedName("seq")
    private Integer seq;

    @SerializedName("type")
    private String debugType;

    @SerializedName("command")
    private String command;

    @Override
    public MessageType<DebugRequest> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<DebugReply> getReplyType() {
        return REPLY_MESSAGE_TYPE;
    }

    public Integer getSeq() {
        return seq;
    }

    public String getDebugType() {
        return debugType;
    }

    public String getCommand() {
        return command;
    }
}
EOF

cat >"${REP_DIR}/DebugReply.java" <<'EOF'
package io.github.spencerpark.jupyter.messages.reply;

import com.google.gson.annotations.SerializedName;
import io.github.spencerpark.jupyter.messages.ContentType;
import io.github.spencerpark.jupyter.messages.MessageType;
import io.github.spencerpark.jupyter.messages.ReplyType;
import io.github.spencerpark.jupyter.messages.request.DebugRequest;

public class DebugReply implements ContentType<DebugReply>, ReplyType<DebugRequest> {
    public static final MessageType<DebugReply> MESSAGE_TYPE = MessageType.DEBUG_REPLY;
    public static final MessageType<DebugRequest> REQUEST_MESSAGE_TYPE = MessageType.DEBUG_REQUEST;

    @SerializedName("type")
    private final String type;

    @SerializedName("request_seq")
    private final Integer requestSeq;

    @SerializedName("success")
    private final boolean success;

    @SerializedName("message")
    private final String message;

    private DebugReply(Integer requestSeq, boolean success, String message) {
        this.type = "response";
        this.requestSeq = requestSeq;
        this.success = success;
        this.message = message;
    }

    public static DebugReply unsupported(Integer requestSeq) {
        return new DebugReply(requestSeq, false, "debugger not supported");
    }

    @Override
    public MessageType<DebugReply> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<DebugRequest> getRequestType() {
        return REQUEST_MESSAGE_TYPE;
    }
}
EOF

cat >"${SRC}/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>io.github.spencerpark</groupId>
    <artifactId>jupyter-jvm-basekernel</artifactId>
    <version>${VERSION}</version>
    <packaging>jar</packaging>
    <properties>
        <maven.compiler.release>8</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.zeromq</groupId>
            <artifactId>jeromq</artifactId>
            <version>0.5.1</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.8.5</version>
        </dependency>
    </dependencies>
</project>
EOF

rm -rf "${SRC}/src/test"

(
	cd "${SRC}"
	mvn -q install -Dmaven.test.skip=true
)

echo "Installed io.github.spencerpark:jupyter-jvm-basekernel:${VERSION}"
