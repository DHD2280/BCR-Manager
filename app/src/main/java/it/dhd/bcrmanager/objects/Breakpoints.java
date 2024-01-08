package it.dhd.bcrmanager.objects;

public class Breakpoints {

    private final String id;
    private final Integer time;
    private String title;
    private String description;

    /**
     * Constructor of a Breakpoint Item
     * @param id The id of the breakpoint
     * @param time The time of the breakpoint
     * @param title The title of the breakpoint
     * @param description The description of the breakpoint
     */
    public Breakpoints(String id, Integer time, String title, String description) {
        this.id = id;
        this.time = time;
        this.title = title;
        this.description = description;
    }

    public String getId() { return id; }
    public Integer getTime() {
        return time;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String title) { this.title=title; }

    public void setDescription(String description) { this.description=description; }
}
