package org.example.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class KopitiamState extends AgentState implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public Optional<String> evaluation() {
        return this.value("evaluation");
    }
}
