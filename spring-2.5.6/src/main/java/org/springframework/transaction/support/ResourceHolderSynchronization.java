/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.support;

/**
 * {@link TransactionSynchronization} implementation that manages a
 * {@link ResourceHolder} bound through {@link TransactionSynchronizationManager}.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 */
public class ResourceHolderSynchronization implements TransactionSynchronization {

	private final ResourceHolder resourceHolder;

	private final Object resourceKey;

	private volatile boolean holderActive = true;


	/**
	 * Create a new ResourceHolderSynchronization for the given holder.
	 * @param resourceHolder the ResourceHolder to manage
	 * @param resourceKey the key to bind the ResourceHolder for
	 * @see TransactionSynchronizationManager#bindResource
	 */
	public ResourceHolderSynchronization(ResourceHolder resourceHolder, Object resourceKey) {
		this.resourceHolder = resourceHolder;
		this.resourceKey = resourceKey;
	}


	public void suspend() {
		if (this.holderActive) {
			TransactionSynchronizationManager.unbindResource(this.resourceKey);
		}
	}

	public void resume() {
		if (this.holderActive) {
			TransactionSynchronizationManager.bindResource(this.resourceKey, this.resourceHolder);
		}
	}

	public void beforeCommit(boolean readOnly) {
	}

	public void beforeCompletion() {
		if (shouldUnbindAtCompletion()) {
			TransactionSynchronizationManager.unbindResource(this.resourceKey);
			this.holderActive = false;
			if (shouldReleaseBeforeCompletion()) {
				releaseResource(this.resourceHolder, this.resourceKey);
			}
		}
	}

	public void afterCommit() {
		if (!shouldReleaseBeforeCompletion()) {
			processResourceAfterCommit(this.resourceHolder);
		}
	}

	public void afterCompletion(int status) {
		if (shouldUnbindAtCompletion()) {
			boolean releaseNecessary = false;
			if (this.holderActive) {
				// The thread-bound resource holder might not be available anymore,
				// since afterCompletion might get called from a different thread.
				this.holderActive = false;
				TransactionSynchronizationManager.unbindResourceIfPossible(this.resourceKey);
				this.resourceHolder.unbound();
				releaseNecessary = true;
			}
			else {
				releaseNecessary = !shouldReleaseBeforeCompletion();
			}
			if (releaseNecessary) {
				releaseResource(this.resourceHolder, this.resourceKey);
			}
		}
		else {
			// Probably a pre-bound resource...
			cleanupResource(this.resourceHolder, this.resourceKey, (status == STATUS_COMMITTED));
		}
		this.resourceHolder.reset();
	}


	/**
	 * Return whether this holder should be unbound at completion
	 * (or should rather be left bound to the thread after the transaction).
	 * <p>The default implementation returns <code>true</code>.
	 */
	protected boolean shouldUnbindAtCompletion() {
		return true;
	}

	/**
	 * Return whether this holder's resource should be released before
	 * transaction completion (<code>true</code>) or rather after
	 * transaction completion (<code>false</code>).
	 * <p>Note that resources will only be released when they are
	 * unbound from the thread ({@link #shouldUnbindAtCompletion()}).
	 * <p>The default implementation returns <code>true</code>.
	 * @see #releaseResource
	 */
	protected boolean shouldReleaseBeforeCompletion() {
		return true;
	}

	/**
	 * After-commit callback for the given resource holder.
	 * Only called when the resource hasn't been released yet
	 * ({@link #shouldReleaseBeforeCompletion()}).
	 * @param resourceHolder the resource holder to process
	 */
	protected void processResourceAfterCommit(ResourceHolder resourceHolder) {
	}

	/**
	 * Release the given resource (after it has been unbound from the thread).
	 * @param resourceHolder the resource holder to process
	 * @param resourceKey the key that the ResourceHolder was bound for
	 */
	protected void releaseResource(ResourceHolder resourceHolder, Object resourceKey) {
	}

	/**
	 * Perform a cleanup on the given resource (which is left bound to the thread).
	 * @param resourceHolder the resource holder to process
	 * @param resourceKey the key that the ResourceHolder was bound for
	 * @param committed whether the transaction has committed (<code>true</code>)
	 * or rolled back (<code>false</code>)
	 */
	protected void cleanupResource(ResourceHolder resourceHolder, Object resourceKey, boolean committed) {
	}

}
