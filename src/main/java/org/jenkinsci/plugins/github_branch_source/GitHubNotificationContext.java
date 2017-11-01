/*
 * The MIT License
 *
 * Copyright 2017 Steven Foster
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.github_branch_source;

import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.kohsuke.github.GHCommitState;

/**
 * Parameter object used in notification strategies {@link AbstractGitHubNotificationStrategy}.
 * @since TODO
 */
public final class GitHubNotificationContext {
    private final Job<?, ?> job;
    private final Run<?, ?> build;
    private final SCMSource source;
    private final SCMHead head;

    /**
     * @since TODO
     */
    private GitHubNotificationContext(Job<?, ?> job, Run<?, ?> build, SCMSource source, SCMHead head) {
        this.job = job;
        this.build = build;
        this.source = source;
        this.head = head;
    }

    public static GitHubNotificationContext build(Job<?, ?> job, Run<?, ?> build, SCMSource source, SCMHead head) {
        return new GitHubNotificationContext(job, build, source, head);
    }

    /**
     * @since TODO
     */
    public Job<?, ?> getJob() {
        return job;
    }

    /**
     * @since TODO
     */
    public Run<?, ?> getBuild() {
        return build;
    }

    /**
     * @since TODO
     */
    public SCMSource getSource() {
        return source;
    }

    /**
     * @since TODO
     */
    public SCMHead getHead() {
        return head;
    }

    /**
     * @since TODO
     */
    @Override
    public String toString() {
        return "GitHubNotificationContext{" +
                "job=" + job +
                ", build=" + build +
                ", source=" + source +
                ", head=" + head +
                '}';
    }

    /**
     * @since TODO
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitHubNotificationContext that = (GitHubNotificationContext) o;

        if (job != null ? !job.equals(that.job) : that.job != null) return false;
        if (build != null ? !build.equals(that.build) : that.build != null) return false;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        return head != null ? head.equals(that.head) : that.head == null;
    }

    /**
     * @since TODO
     */
    @Override
    public int hashCode() {
        int result = job != null ? job.hashCode() : 0;
        result = 31 * result + (build != null ? build.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (head != null ? head.hashCode() : 0);
        return result;
    }

    /**
     * Retrieves default context
     * @param listener Listener for the build, if any
     * @return Default notification context
     * @since TODO
     */
    public String getDefaultContext(TaskListener listener) {
        if (head instanceof PullRequestSCMHead) {
            if (((PullRequestSCMHead) head).isMerge()) {
                return "continuous-integration/jenkins/pr-merge";
            } else {
                return "continuous-integration/jenkins/pr-head";
            }
        } else {
            return "continuous-integration/jenkins/branch";
        }
    }

    /**
     * Retrieves default URL
     * @param listener Listener for the build, if any
     * @return Default notification URL backref
     * @since TODO
     */
    public String getDefaultUrl(TaskListener listener) {
        String url = null;
        try {
            if (null != build) {
                url = DisplayURLProvider.get().getRunURL(build);
            }
            else if (null != job) {
                url = DisplayURLProvider.get().getJobURL(job);
            }
        } catch (IllegalStateException e) {
            listener.getLogger().println(
                    "Can not determine Jenkins root URL. Commit status notifications are disabled "
                            + "until a root URL is"
                            + " configured in Jenkins global configuration.");
        }
        return url;
    }

    /**
     * Retrieves default notification message
     * @param listener Listener for the build, if any
     * @return Default notification message
     * @since TODO
     */
    public String getDefaultMessage(TaskListener listener) {
        if (null != build) {
            Result result = build.getResult();
            if (Result.SUCCESS.equals(result)) {
                return Messages.GitHubBuildStatusNotification_CommitStatus_Good();
            } else if (Result.UNSTABLE.equals(result)) {
                return Messages.GitHubBuildStatusNotification_CommitStatus_Unstable();
            } else if (Result.FAILURE.equals(result)) {
                return Messages.GitHubBuildStatusNotification_CommitStatus_Failure();
            } else if (Result.ABORTED.equals(result)) {
                return Messages.GitHubBuildStatusNotification_CommitStatus_Aborted();
            } else if (result != null) { // NOT_BUILT etc.
                return Messages.GitHubBuildStatusNotification_CommitStatus_Other();
            } else {
                return Messages.GitHubBuildStatusNotification_CommitStatus_Pending();
            }
        }
        return Messages.GitHubBuildStatusNotification_CommitStatus_Queued();
    }

    /**
     * Retrieves default notification state
     * @param listener Listener for the build, if any
     * @return Default notification state
     * @since TODO
     */
    public GHCommitState getDefaultState(TaskListener listener) {
        if (null != build) {
            Result result = build.getResult();
            if (Result.SUCCESS.equals(result)) {
                return GHCommitState.SUCCESS;
            } else if (Result.UNSTABLE.equals(result)) {
                return GHCommitState.FAILURE;
            } else if (Result.FAILURE.equals(result)) {
                return GHCommitState.ERROR;
            } else if (Result.ABORTED.equals(result)) {
                return GHCommitState.ERROR;
            } else if (result != null) { // NOT_BUILT etc.
                return GHCommitState.ERROR;
            }
        }
        return GHCommitState.PENDING;
    }

    /**
     * Retrieves whether plugin should ignore errors when updating the GitHub status
     * @param listener Listener for the build, if any
     * @return Default ignore errors policy
     * @since TODO
     */
    public boolean getDefaultIgnoreError(TaskListener listener) {
        return null == build || null == build.getResult();
    }
}
