package utils;

public enum LoadBalancingPolicy {
    RANDOM("random"),
    ROUNDROBIN("roundrobin"),
    LEASTCONN("leastconn");

    private String name = "";

    LoadBalancingPolicy(String name){
        this.name = name;
    }

    public String toString(){
        return name;
    }
}
