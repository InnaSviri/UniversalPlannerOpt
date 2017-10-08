package ru.programpark.entity.train;

import ru.programpark.entity.fixed.Link;

public class TrainDepart extends TrainState {
    private Link link;

    public TrainDepart() {
    }

    public TrainDepart(Long id, Long time, Link link) {
        super(id, time);
        this.link = link;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public String toString() {
        return "TrainDepart{" + link.getFrom().getId() +
            " → " + link.getTo().getId() + ", " + getTime() + "}";
    }
}
