package me.marquinho.protectedareaclient.client.debug;

import java.util.ArrayList;
import java.util.List;

public class DebugState {

    public static final int PAGE_OVERVIEW   = 0;
    public static final int PAGE_RULES      = 1;
    public static final int PAGE_EXCEPTIONS = 2;
    public static final int PAGE_LIMIT      = 3;
    public static final int PAGE_ADVANCED   = 4;

    private boolean active = false;
    private int currentPage = PAGE_OVERVIEW;
    private int totalPages  = 5;
    private String currentAreaId = "";
    private String debugType = "cube";

    private int totalAreas = 0;

    private final List<RuleEntry> rules = new ArrayList<>();

    private final List<ExceptionEntry> exceptions = new ArrayList<>();

    private boolean hasLimit = false;
    private int limitMax = 0;
    private int limitCurrent = 0;
    private boolean limitBlocked = false;
    private boolean limitException = false;

    private final List<AdvancedEntry> advancedRules = new ArrayList<>();

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active      = false;
        this.currentPage = PAGE_OVERVIEW;
        this.currentAreaId = "";
        this.totalAreas  = 0;
        this.debugType   = "cube";
        this.rules.clear();
        this.exceptions.clear();
        this.hasLimit    = false;
        this.limitMax    = 0;
        this.limitCurrent = 0;
        this.limitBlocked = false;
        this.limitException = false;
        this.advancedRules.clear();
    }

    public void toggleDebugType() {
        debugType = "cube".equals(debugType) ? "flat" : "cube";
        currentPage = PAGE_OVERVIEW;
    }

    public int nextPage() {
        if ("flat".equals(debugType)) return currentPage;
        currentPage = (currentPage + 1) % totalPages;
        return currentPage;
    }

    public int prevPage() {
        if ("flat".equals(debugType)) return currentPage;
        currentPage = (currentPage - 1 + totalPages) % totalPages;
        return currentPage;
    }

    public void applyOverviewData(String currentAreaId, int totalAreas, int totalPages) {
        this.currentAreaId = currentAreaId;
        this.totalAreas    = totalAreas;
        this.totalPages    = totalPages;
    }

    public void applyRulesData(String currentAreaId, List<RuleEntry> rules, int totalPages) {
        this.currentAreaId = currentAreaId;
        this.totalPages    = totalPages;
        this.rules.clear();
        this.rules.addAll(rules);
    }

    public void applyExceptionsData(String currentAreaId, List<ExceptionEntry> exceptions, int totalPages) {
        this.currentAreaId = currentAreaId;
        this.totalPages    = totalPages;
        this.exceptions.clear();
        this.exceptions.addAll(exceptions);
    }

    public void applyLimitData(String currentAreaId, boolean hasLimit, int limitMax,
                               int limitCurrent, boolean limitBlocked, boolean limitException, int totalPages) {
        this.currentAreaId  = currentAreaId;
        this.totalPages     = totalPages;
        this.hasLimit       = hasLimit;
        this.limitMax       = limitMax;
        this.limitCurrent   = limitCurrent;
        this.limitBlocked   = limitBlocked;
        this.limitException = limitException;
    }

    public void applyAdvancedData(String currentAreaId, List<AdvancedEntry> advancedRules, int totalPages) {
        this.currentAreaId = currentAreaId;
        this.totalPages    = totalPages;
        this.advancedRules.clear();
        this.advancedRules.addAll(advancedRules);
    }

    public String getDebugType()            { return debugType; }
    public boolean isActive()              { return active; }
    public int getCurrentPage()            { return currentPage; }
    public int getTotalPages()             { return totalPages; }
    public String getCurrentAreaId()       { return currentAreaId; }
    public int getTotalAreas()             { return totalAreas; }
    public List<RuleEntry> getRules()      { return rules; }
    public List<ExceptionEntry> getExceptions() { return exceptions; }
    public boolean isHasLimit()            { return hasLimit; }
    public int getLimitMax()               { return limitMax; }
    public int getLimitCurrent()           { return limitCurrent; }
    public boolean isLimitBlocked()        { return limitBlocked; }
    public boolean isLimitException()      { return limitException; }
    public List<AdvancedEntry> getAdvancedRules() { return advancedRules; }

    public record RuleEntry(String key, boolean hasException) {}
    public record ExceptionEntry(String ruleKey, String playerName) {}
    public record AdvancedEntry(String ruleType, String targetType, String targetId) {}
}