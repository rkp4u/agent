package org.example.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * KopitiamState - Java equivalent of Python's State(TypedDict).
 * "messages" uses an AppenderChannel so each node's returned list
 * is appended to the accumulated history rather than replacing it.
 */
public class KopitiamState extends AgentState {

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            "messages", Channels.appender(ArrayList::new)
    );

    public KopitiamState(Map<String, Object> initData) {
        super(initData);
    }

    public List<Map<String, String>> messages() {
        return this.<List<Map<String, String>>>value("messages").orElseGet(List::of);
    }

    public int volleyMsgLeft() {
        return this.<Integer>value("volley_msg_left").orElse(0);
    }

    public Optional<String> nextSpeaker() {
        return this.value("next_speaker");
    }
}
