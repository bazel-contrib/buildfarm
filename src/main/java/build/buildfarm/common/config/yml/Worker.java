package build.buildfarm.common.config.yml;

public class Worker {
    private long port;
    private String publicName;
    private Capabilities capabilities;
    private String root;
    private long inlineContentLimit;
    private long operationPollPeriod;
    private DequeueMatchSettings dequeueMatchSettings;
    private Cas cas;
    private long executeStageWidth;
    private long inputFetchStageWidth;
    private long inputFetchDeadline;
    private boolean linkInputDirectories;
    private String realInputDirectories;
    private long defaultActionTimeout;
    private long maximumActionTimeout;
    private Object execOwner;

    public long getPort() {
        return port;
    }

    public void setPort(long port) {
        this.port = port;
    }

    public String getPublicName() {
        return publicName;
    }

    public void setPublicName(String publicName) {
        this.publicName = publicName;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public long getInlineContentLimit() {
        return inlineContentLimit;
    }

    public void setInlineContentLimit(long inlineContentLimit) {
        this.inlineContentLimit = inlineContentLimit;
    }

    public long getOperationPollPeriod() {
        return operationPollPeriod;
    }

    public void setOperationPollPeriod(long operationPollPeriod) {
        this.operationPollPeriod = operationPollPeriod;
    }

    public DequeueMatchSettings getDequeueMatchSettings() {
        return dequeueMatchSettings;
    }

    public void setDequeueMatchSettings(DequeueMatchSettings dequeueMatchSettings) {
        this.dequeueMatchSettings = dequeueMatchSettings;
    }

    public Cas getCas() {
        return cas;
    }

    public void setCas(Cas cas) {
        this.cas = cas;
    }

    public long getExecuteStageWidth() {
        return executeStageWidth;
    }

    public void setExecuteStageWidth(long executeStageWidth) {
        this.executeStageWidth = executeStageWidth;
    }

    public long getInputFetchStageWidth() {
        return inputFetchStageWidth;
    }

    public void setInputFetchStageWidth(long inputFetchStageWidth) {
        this.inputFetchStageWidth = inputFetchStageWidth;
    }

    public long getInputFetchDeadline() {
        return inputFetchDeadline;
    }

    public void setInputFetchDeadline(long inputFetchDeadline) {
        this.inputFetchDeadline = inputFetchDeadline;
    }

    public boolean isLinkInputDirectories() {
        return linkInputDirectories;
    }

    public void setLinkInputDirectories(boolean linkInputDirectories) {
        this.linkInputDirectories = linkInputDirectories;
    }

    public String getRealInputDirectories() {
        return realInputDirectories;
    }

    public void setRealInputDirectories(String realInputDirectories) {
        this.realInputDirectories = realInputDirectories;
    }

    public long getDefaultActionTimeout() {
        return defaultActionTimeout;
    }

    public void setDefaultActionTimeout(long defaultActionTimeout) {
        this.defaultActionTimeout = defaultActionTimeout;
    }

    public long getMaximumActionTimeout() {
        return maximumActionTimeout;
    }

    public void setMaximumActionTimeout(long maximumActionTimeout) {
        this.maximumActionTimeout = maximumActionTimeout;
    }

    public Object getExecOwner() {
        return execOwner;
    }

    public void setExecOwner(Object execOwner) {
        this.execOwner = execOwner;
    }

    @Override
    public String toString() {
        return "Worker{" +
                "port=" + port +
                ", publicName='" + publicName + '\'' +
                ", capabilities=" + capabilities +
                ", root='" + root + '\'' +
                ", inlineContentLimit=" + inlineContentLimit +
                ", operationPollPeriod=" + operationPollPeriod +
                ", dequeueMatchSettings=" + dequeueMatchSettings +
                ", cas=" + cas +
                ", executeStageWidth=" + executeStageWidth +
                ", inputFetchStageWidth=" + inputFetchStageWidth +
                ", inputFetchDeadline=" + inputFetchDeadline +
                ", linkInputDirectories=" + linkInputDirectories +
                ", realInputDirectories='" + realInputDirectories + '\'' +
                ", defaultActionTimeout=" + defaultActionTimeout +
                ", maximumActionTimeout=" + maximumActionTimeout +
                ", execOwner=" + execOwner +
                '}';
    }
}
