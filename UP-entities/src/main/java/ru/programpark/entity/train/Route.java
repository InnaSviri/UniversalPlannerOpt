package ru.programpark.entity.train;

import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * User: olga
 * Date: 13.05.14
 */
public class Route implements Serializable{
    private List<Link> linkList = new ArrayList<Link>();//routes link list

    public Route(List<Link> linkList){
        for (Link link: linkList){
            this.linkList.add(link);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Route route1 = (Route) o;

        if (linkList != null) {
            if (linkList.size() != route1.linkList.size())
                return false;
            for (int i=0; i< linkList.size(); i++){
                if (linkList.get(i).equals(route1.linkList.get(i)))
                    return false;
            }
        } else if (route1.linkList != null)
            return false;

        return true;
    }

    public boolean containsLink(Link link){
        for (Link l: linkList){
            if (l.equals(link)){
                return true;
            }
        }

        return false;
    }

    public boolean containsStation(Station S){
        for (Link l: linkList)
            if (l.getTo().equals(S) || l.getFrom().equals(S))
                return true;
        return false;
    }

    @Override
    public int hashCode() {
        int result = linkList.get(0) != null ? linkList.get(0).hashCode() : 0;
        for (int i=1; i< linkList.size(); i++){
            result = 31 * result + (linkList.get(i) != null ? linkList.get(i).hashCode() : 0);
        }
        return result;
    }

    public List<Link> getLinkList() {
        return linkList;
    }

    public void addLink(Link l){
        linkList.add(l);
    }

    public void setLinkList(List<Link> linkList) {
        this.linkList = linkList;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Route");
        sb.append("{linkList=").append(linkList);
        sb.append('}');
        return sb.toString();
    }

    public List<OneTaskTrack> toOneTaskRoute(){
        List<OneTaskTrack> result = new ArrayList();

        for (Link link: linkList){
            OneTaskTrack track = new OneTaskTrack(link, Long.MIN_VALUE, Long.MIN_VALUE, -1L);
            result.add(track);
        }

        return result;
    }
}
