package me.darknet.assembler.query;

/**
 * Kind of reference to a label.
 */
public enum LabelReferenceKind {
	FLOW,
	SWITCH_DEFAULT,
	SWITCH_CASE,
	TRY_START,
	TRY_END,
	HANDLER
}
