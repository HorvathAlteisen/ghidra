/* ###
 * IP: GHIDRA
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
package ghidra.app.util.pdb;

import java.util.ArrayList;
import java.util.List;

import ghidra.app.util.SymbolPath;
import ghidra.program.model.symbol.SymbolUtilities;

public class PdbNamespaceUtils {

	/**
	 * Placeholder substituted for a complex type (composite or enum) whose name is blank.
	 * <P>
	 * Newer MSVC toolchains emit a marker such as {@code <unnamed-tag>} for anonymous types, but
	 * some older ones (e.g. VC6/VC98) emit a genuinely empty name.  A blank name causes
	 * {@link ghidra.app.util.SymbolPathParser#parse(String)} to throw
	 * {@link IllegalArgumentException} ("Pathname cannot be empty!"), which aborts PDB analysis.
	 * Substituting this placeholder before parsing both avoids that exception and routes the type
	 * through {@link #fixUnnamed(String, int)}, where it is given a record-number suffix so each
	 * anonymous type becomes a distinct, named data type (e.g. {@code <unnamed-tag_0000A3F1>})
	 * rather than being lost or collapsed together.
	 */
	public static final String UNNAMED_TAG = "<unnamed-tag>";

	/**
	 * Returns {@code name}, or {@link #UNNAMED_TAG} if {@code name} is {@code null} or blank.
	 * Use this to guard {@link ghidra.app.util.SymbolPathParser#parse(String)} against blank
	 * complex-type names (see {@link #UNNAMED_TAG} for details).
	 * @param name the complex type name, possibly blank
	 * @return a non-blank name suitable for symbol-path parsing
	 */
	public static String nameOrUnnamedTag(String name) {
		return (name == null || name.isBlank()) ? UNNAMED_TAG : name;
	}

	/**
	 * Fixes {@link SymbolPath} name, eliminating invalid characters and making the terminal
	 *  name of the namespace unique by the index number when necessary.  For example,
	 *  anonymous and unnamed components such as {@code <unnamed-tag>} and {@code <unnamed-type>}
	 *  are fixed up. Example:
	 * <pre>
	 *   {@code _SYSTEM_INFO::<unnamed-tag>}
	 * </pre>
	 * @param symbolPath the source {@link SymbolPath}
	 * @param index the index number used be used as part of a unique tag name.
	 * @return the resulting {@link SymbolPath}
	 */
	public static SymbolPath convertToGhidraPathName(SymbolPath symbolPath, int index) {
		symbolPath = symbolPath.replaceInvalidChars();
		return new SymbolPath(symbolPath.getParent(), fixUnnamed(symbolPath.getName(), index));
	}

	/**
	 * Fixes {@link SymbolPath} name, eliminating invalid characters
	 * @param symbolPath the source {@link SymbolPath}
	 * @return the resulting {@link SymbolPath}
	 */
	public static SymbolPath convertToGhidraPathName(SymbolPath symbolPath) {
		symbolPath = symbolPath.replaceInvalidChars();
		return symbolPath;
	}

	/**
	 * Fixes {@code <unnamed-tag>} and {@code <unnamed-type>} components of a namespace.
	 * <P>
	 * NOTE: This could be an issue when there are multiple unnamed items, such as in, for example:
	 * <pre>
	 *   {@code _SYSTEM_INFO::<unnamed-tag>::<unnamed-tag>}
	 * </pre>
	 * @param symbolPath the source {@link SymbolPath}
	 * @param index the index number used be used as part of a unique tag name.
	 * @return the resulting {@link SymbolPath}
	 */
	public static SymbolPath convertToGhidraPath(SymbolPath symbolPath, int index) {
		symbolPath = symbolPath.replaceInvalidChars();
		List<String> modList = new ArrayList<>();
		for (String str : symbolPath.asList()) {
			modList.add(SymbolUtilities.replaceInvalidChars(fixUnnamed(str, index), true));
		}
		return new SymbolPath(modList);
//		return getFixUpSymbolPathRecurse(symbolPath, index);
	}

//	private static SymbolPath getFixUpSymbolPathRecurse(SymbolPath symbolPath, int index) {
//		SymbolPath parent = symbolPath.getParent();
//		if (parent != null) {
//			parent = getFixUpSymbolPathRecurse(parent, index);
//		}
//		return new SymbolPath(parent, fixName(symbolPath.getName(), index));
//	}
//
	/**
	 * Fixes-up {@code <unnamed-tag>} or {@code <unnamed-type>} component of a name.
	 * @param name original name.
	 * @param index the index number used be used as part of a unique tag name.
	 * @return the resulting name.
	 */
	// TODO: investigate if we can work a solution that no longer needs/has the index attached.
	// I don't believe we are handling all of these correctly... someone else suggests regex, but
	// that makes work in this area less clear until we know what we are doing...
	// so, for now... DO NOT DO REGEX.
	public static String fixUnnamed(String name, int index) {
		if ("<unnamed-tag>".equals(name)) {
			return String.format("<unnamed-tag_%08X>", index);
		}
		if ("<anonymous-tag>".equals(name)) {
			return String.format("<anonymous-tag_%08X>", index);
		}
		if ("<unnamed-type>".equals(name)) {
			return String.format("<unnamed-type_%08X>", index);
		}
		if ("__unnamed".equals(name)) {
			return String.format("__unnamed_%08X", index);
		}
		return name;
	}

}
