package com.smartfoo.android.core.app;

public interface GenericPromptSingleButtonDialogFragmentCallbacks
{
    /**
     * @param dialogFragment
     * @param fragmentTagName
     * @return true if handled, false if not handled
     */
    boolean onGenericPromptSingleButtonDialogFragmentResult(GenericPromptSingleButtonDialogFragment dialogFragment,
                                                            String fragmentTagName);
}
