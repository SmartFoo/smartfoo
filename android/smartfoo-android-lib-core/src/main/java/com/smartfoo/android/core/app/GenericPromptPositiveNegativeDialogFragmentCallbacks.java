package com.smartfoo.android.core.app;

public interface GenericPromptPositiveNegativeDialogFragmentCallbacks
{
    /**
     * @param dialogFragment
     * @param fragmentTagName
     * @return true if handled, false if not handled
     */
    boolean onGenericPromptPositiveNegativeDialogFragmentResult(GenericPromptPositiveNegativeDialogFragment dialogFragment,
                                                                String fragmentTagName);
}
