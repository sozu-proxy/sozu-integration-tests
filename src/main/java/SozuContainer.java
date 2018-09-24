import org.testcontainers.containers.GenericContainer;

public class SozuContainer <SELF extends SozuContainer<SELF>> extends GenericContainer<SELF> {

    public SozuContainer() {
        super("sozu:latest");
    }

    @Override
    protected void configure() {
    }
}
