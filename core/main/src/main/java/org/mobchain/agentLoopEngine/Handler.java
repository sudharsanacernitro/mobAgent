package org.mobchain.agentLoopEngine;

public abstract class Handler {

    private Handler next;

    public Handler link(Handler next) {

        this.next = next;
        return next;

    }

    public  void handle(MsgContext ctx) throws Exception {

        if (canHandle(ctx)) {

            process(ctx);

        }

        if((ctx.has("ResponseContent")) || ( ctx.has("exception") && ctx.get("exception",Boolean.class )) ) {

            return;

        }

        if (next != null) {

            next.handle(ctx);

        }
    }

    protected abstract boolean canHandle(MsgContext ctx);
    protected abstract void process(MsgContext ctx) throws Exception;
}
