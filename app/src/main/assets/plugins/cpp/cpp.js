// node_modules/@lezer/common/dist/index.js
var DefaultBufferLength = 1024;
var nextPropID = 0;
var Range = class {
  constructor(from, to) {
    this.from = from;
    this.to = to;
  }
};
var NodeProp = class {
  /**
  Create a new node prop type.
  */
  constructor(config = {}) {
    this.id = nextPropID++;
    this.perNode = !!config.perNode;
    this.deserialize = config.deserialize || (() => {
      throw new Error("This node type doesn't define a deserialize function");
    });
    this.combine = config.combine || null;
  }
  /**
  This is meant to be used with
  [`NodeSet.extend`](#common.NodeSet.extend) or
  [`LRParser.configure`](#lr.ParserConfig.props) to compute
  prop values for each node type in the set. Takes a [match
  object](#common.NodeType^match) or function that returns undefined
  if the node type doesn't get this prop, and the prop's value if
  it does.
  */
  add(match) {
    if (this.perNode)
      throw new RangeError("Can't add per-node props to node types");
    if (typeof match != "function")
      match = NodeType.match(match);
    return (type) => {
      let result = match(type);
      return result === void 0 ? null : [this, result];
    };
  }
};
NodeProp.closedBy = new NodeProp({ deserialize: (str) => str.split(" ") });
NodeProp.openedBy = new NodeProp({ deserialize: (str) => str.split(" ") });
NodeProp.group = new NodeProp({ deserialize: (str) => str.split(" ") });
NodeProp.isolate = new NodeProp({ deserialize: (value) => {
  if (value && value != "rtl" && value != "ltr" && value != "auto")
    throw new RangeError("Invalid value for isolate: " + value);
  return value || "auto";
} });
NodeProp.contextHash = new NodeProp({ perNode: true });
NodeProp.lookAhead = new NodeProp({ perNode: true });
NodeProp.mounted = new NodeProp({ perNode: true });
var MountedTree = class {
  constructor(tree, overlay, parser2) {
    this.tree = tree;
    this.overlay = overlay;
    this.parser = parser2;
  }
  /**
  @internal
  */
  static get(tree) {
    return tree && tree.props && tree.props[NodeProp.mounted.id];
  }
};
var noProps = /* @__PURE__ */ Object.create(null);
var NodeType = class _NodeType {
  /**
  @internal
  */
  constructor(name2, props, id, flags = 0) {
    this.name = name2;
    this.props = props;
    this.id = id;
    this.flags = flags;
  }
  /**
  Define a node type.
  */
  static define(spec) {
    let props = spec.props && spec.props.length ? /* @__PURE__ */ Object.create(null) : noProps;
    let flags = (spec.top ? 1 : 0) | (spec.skipped ? 2 : 0) | (spec.error ? 4 : 0) | (spec.name == null ? 8 : 0);
    let type = new _NodeType(spec.name || "", props, spec.id, flags);
    if (spec.props)
      for (let src of spec.props) {
        if (!Array.isArray(src))
          src = src(type);
        if (src) {
          if (src[0].perNode)
            throw new RangeError("Can't store a per-node prop on a node type");
          props[src[0].id] = src[1];
        }
      }
    return type;
  }
  /**
  Retrieves a node prop for this type. Will return `undefined` if
  the prop isn't present on this node.
  */
  prop(prop) {
    return this.props[prop.id];
  }
  /**
  True when this is the top node of a grammar.
  */
  get isTop() {
    return (this.flags & 1) > 0;
  }
  /**
  True when this node is produced by a skip rule.
  */
  get isSkipped() {
    return (this.flags & 2) > 0;
  }
  /**
  Indicates whether this is an error node.
  */
  get isError() {
    return (this.flags & 4) > 0;
  }
  /**
  When true, this node type doesn't correspond to a user-declared
  named node, for example because it is used to cache repetition.
  */
  get isAnonymous() {
    return (this.flags & 8) > 0;
  }
  /**
  Returns true when this node's name or one of its
  [groups](#common.NodeProp^group) matches the given string.
  */
  is(name2) {
    if (typeof name2 == "string") {
      if (this.name == name2)
        return true;
      let group = this.prop(NodeProp.group);
      return group ? group.indexOf(name2) > -1 : false;
    }
    return this.id == name2;
  }
  /**
  Create a function from node types to arbitrary values by
  specifying an object whose property names are node or
  [group](#common.NodeProp^group) names. Often useful with
  [`NodeProp.add`](#common.NodeProp.add). You can put multiple
  names, separated by spaces, in a single property name to map
  multiple node names to a single value.
  */
  static match(map) {
    let direct = /* @__PURE__ */ Object.create(null);
    for (let prop in map)
      for (let name2 of prop.split(" "))
        direct[name2] = map[prop];
    return (node) => {
      for (let groups = node.prop(NodeProp.group), i = -1; i < (groups ? groups.length : 0); i++) {
        let found = direct[i < 0 ? node.name : groups[i]];
        if (found)
          return found;
      }
    };
  }
};
NodeType.none = new NodeType(
  "",
  /* @__PURE__ */ Object.create(null),
  0,
  8
  /* NodeFlag.Anonymous */
);
var NodeSet = class _NodeSet {
  /**
  Create a set with the given types. The `id` property of each
  type should correspond to its position within the array.
  */
  constructor(types2) {
    this.types = types2;
    for (let i = 0; i < types2.length; i++)
      if (types2[i].id != i)
        throw new RangeError("Node type ids should correspond to array positions when creating a node set");
  }
  /**
  Create a copy of this set with some node properties added. The
  arguments to this method can be created with
  [`NodeProp.add`](#common.NodeProp.add).
  */
  extend(...props) {
    let newTypes = [];
    for (let type of this.types) {
      let newProps = null;
      for (let source of props) {
        let add = source(type);
        if (add) {
          if (!newProps)
            newProps = Object.assign({}, type.props);
          let value = add[1], prop = add[0];
          if (prop.combine && prop.id in newProps)
            value = prop.combine(newProps[prop.id], value);
          newProps[prop.id] = value;
        }
      }
      newTypes.push(newProps ? new NodeType(type.name, newProps, type.id, type.flags) : type);
    }
    return new _NodeSet(newTypes);
  }
};
var CachedNode = /* @__PURE__ */ new WeakMap();
var CachedInnerNode = /* @__PURE__ */ new WeakMap();
var IterMode;
(function(IterMode2) {
  IterMode2[IterMode2["ExcludeBuffers"] = 1] = "ExcludeBuffers";
  IterMode2[IterMode2["IncludeAnonymous"] = 2] = "IncludeAnonymous";
  IterMode2[IterMode2["IgnoreMounts"] = 4] = "IgnoreMounts";
  IterMode2[IterMode2["IgnoreOverlays"] = 8] = "IgnoreOverlays";
})(IterMode || (IterMode = {}));
var Tree = class _Tree {
  /**
  Construct a new tree. See also [`Tree.build`](#common.Tree^build).
  */
  constructor(type, children, positions, length, props) {
    this.type = type;
    this.children = children;
    this.positions = positions;
    this.length = length;
    this.props = null;
    if (props && props.length) {
      this.props = /* @__PURE__ */ Object.create(null);
      for (let [prop, value] of props)
        this.props[typeof prop == "number" ? prop : prop.id] = value;
    }
  }
  /**
  @internal
  */
  toString() {
    let mounted = MountedTree.get(this);
    if (mounted && !mounted.overlay)
      return mounted.tree.toString();
    let children = "";
    for (let ch of this.children) {
      let str = ch.toString();
      if (str) {
        if (children)
          children += ",";
        children += str;
      }
    }
    return !this.type.name ? children : (/\W/.test(this.type.name) && !this.type.isError ? JSON.stringify(this.type.name) : this.type.name) + (children.length ? "(" + children + ")" : "");
  }
  /**
  Get a [tree cursor](#common.TreeCursor) positioned at the top of
  the tree. Mode can be used to [control](#common.IterMode) which
  nodes the cursor visits.
  */
  cursor(mode = 0) {
    return new TreeCursor(this.topNode, mode);
  }
  /**
  Get a [tree cursor](#common.TreeCursor) pointing into this tree
  at the given position and side (see
  [`moveTo`](#common.TreeCursor.moveTo).
  */
  cursorAt(pos, side = 0, mode = 0) {
    let scope = CachedNode.get(this) || this.topNode;
    let cursor = new TreeCursor(scope);
    cursor.moveTo(pos, side);
    CachedNode.set(this, cursor._tree);
    return cursor;
  }
  /**
  Get a [syntax node](#common.SyntaxNode) object for the top of the
  tree.
  */
  get topNode() {
    return new TreeNode(this, 0, 0, null);
  }
  /**
  Get the [syntax node](#common.SyntaxNode) at the given position.
  If `side` is -1, this will move into nodes that end at the
  position. If 1, it'll move into nodes that start at the
  position. With 0, it'll only enter nodes that cover the position
  from both sides.
  
  Note that this will not enter
  [overlays](#common.MountedTree.overlay), and you often want
  [`resolveInner`](#common.Tree.resolveInner) instead.
  */
  resolve(pos, side = 0) {
    let node = resolveNode(CachedNode.get(this) || this.topNode, pos, side, false);
    CachedNode.set(this, node);
    return node;
  }
  /**
  Like [`resolve`](#common.Tree.resolve), but will enter
  [overlaid](#common.MountedTree.overlay) nodes, producing a syntax node
  pointing into the innermost overlaid tree at the given position
  (with parent links going through all parent structure, including
  the host trees).
  */
  resolveInner(pos, side = 0) {
    let node = resolveNode(CachedInnerNode.get(this) || this.topNode, pos, side, true);
    CachedInnerNode.set(this, node);
    return node;
  }
  /**
  In some situations, it can be useful to iterate through all
  nodes around a position, including those in overlays that don't
  directly cover the position. This method gives you an iterator
  that will produce all nodes, from small to big, around the given
  position.
  */
  resolveStack(pos, side = 0) {
    return stackIterator(this, pos, side);
  }
  /**
  Iterate over the tree and its children, calling `enter` for any
  node that touches the `from`/`to` region (if given) before
  running over such a node's children, and `leave` (if given) when
  leaving the node. When `enter` returns `false`, that node will
  not have its children iterated over (or `leave` called).
  */
  iterate(spec) {
    let { enter, leave, from = 0, to = this.length } = spec;
    let mode = spec.mode || 0, anon = (mode & IterMode.IncludeAnonymous) > 0;
    for (let c = this.cursor(mode | IterMode.IncludeAnonymous); ; ) {
      let entered = false;
      if (c.from <= to && c.to >= from && (!anon && c.type.isAnonymous || enter(c) !== false)) {
        if (c.firstChild())
          continue;
        entered = true;
      }
      for (; ; ) {
        if (entered && leave && (anon || !c.type.isAnonymous))
          leave(c);
        if (c.nextSibling())
          break;
        if (!c.parent())
          return;
        entered = true;
      }
    }
  }
  /**
  Get the value of the given [node prop](#common.NodeProp) for this
  node. Works with both per-node and per-type props.
  */
  prop(prop) {
    return !prop.perNode ? this.type.prop(prop) : this.props ? this.props[prop.id] : void 0;
  }
  /**
  Returns the node's [per-node props](#common.NodeProp.perNode) in a
  format that can be passed to the [`Tree`](#common.Tree)
  constructor.
  */
  get propValues() {
    let result = [];
    if (this.props)
      for (let id in this.props)
        result.push([+id, this.props[id]]);
    return result;
  }
  /**
  Balance the direct children of this tree, producing a copy of
  which may have children grouped into subtrees with type
  [`NodeType.none`](#common.NodeType^none).
  */
  balance(config = {}) {
    return this.children.length <= 8 ? this : balanceRange(NodeType.none, this.children, this.positions, 0, this.children.length, 0, this.length, (children, positions, length) => new _Tree(this.type, children, positions, length, this.propValues), config.makeTree || ((children, positions, length) => new _Tree(NodeType.none, children, positions, length)));
  }
  /**
  Build a tree from a postfix-ordered buffer of node information,
  or a cursor over such a buffer.
  */
  static build(data) {
    return buildTree(data);
  }
};
Tree.empty = new Tree(NodeType.none, [], [], 0);
var FlatBufferCursor = class _FlatBufferCursor {
  constructor(buffer, index) {
    this.buffer = buffer;
    this.index = index;
  }
  get id() {
    return this.buffer[this.index - 4];
  }
  get start() {
    return this.buffer[this.index - 3];
  }
  get end() {
    return this.buffer[this.index - 2];
  }
  get size() {
    return this.buffer[this.index - 1];
  }
  get pos() {
    return this.index;
  }
  next() {
    this.index -= 4;
  }
  fork() {
    return new _FlatBufferCursor(this.buffer, this.index);
  }
};
var TreeBuffer = class _TreeBuffer {
  /**
  Create a tree buffer.
  */
  constructor(buffer, length, set) {
    this.buffer = buffer;
    this.length = length;
    this.set = set;
  }
  /**
  @internal
  */
  get type() {
    return NodeType.none;
  }
  /**
  @internal
  */
  toString() {
    let result = [];
    for (let index = 0; index < this.buffer.length; ) {
      result.push(this.childString(index));
      index = this.buffer[index + 3];
    }
    return result.join(",");
  }
  /**
  @internal
  */
  childString(index) {
    let id = this.buffer[index], endIndex = this.buffer[index + 3];
    let type = this.set.types[id], result = type.name;
    if (/\W/.test(result) && !type.isError)
      result = JSON.stringify(result);
    index += 4;
    if (endIndex == index)
      return result;
    let children = [];
    while (index < endIndex) {
      children.push(this.childString(index));
      index = this.buffer[index + 3];
    }
    return result + "(" + children.join(",") + ")";
  }
  /**
  @internal
  */
  findChild(startIndex, endIndex, dir, pos, side) {
    let { buffer } = this, pick = -1;
    for (let i = startIndex; i != endIndex; i = buffer[i + 3]) {
      if (checkSide(side, pos, buffer[i + 1], buffer[i + 2])) {
        pick = i;
        if (dir > 0)
          break;
      }
    }
    return pick;
  }
  /**
  @internal
  */
  slice(startI, endI, from) {
    let b = this.buffer;
    let copy = new Uint16Array(endI - startI), len = 0;
    for (let i = startI, j = 0; i < endI; ) {
      copy[j++] = b[i++];
      copy[j++] = b[i++] - from;
      let to = copy[j++] = b[i++] - from;
      copy[j++] = b[i++] - startI;
      len = Math.max(len, to);
    }
    return new _TreeBuffer(copy, len, this.set);
  }
};
function checkSide(side, pos, from, to) {
  switch (side) {
    case -2:
      return from < pos;
    case -1:
      return to >= pos && from < pos;
    case 0:
      return from < pos && to > pos;
    case 1:
      return from <= pos && to > pos;
    case 2:
      return to > pos;
    case 4:
      return true;
  }
}
function resolveNode(node, pos, side, overlays) {
  var _a2;
  while (node.from == node.to || (side < 1 ? node.from >= pos : node.from > pos) || (side > -1 ? node.to <= pos : node.to < pos)) {
    let parent = !overlays && node instanceof TreeNode && node.index < 0 ? null : node.parent;
    if (!parent)
      return node;
    node = parent;
  }
  let mode = overlays ? 0 : IterMode.IgnoreOverlays;
  if (overlays)
    for (let scan = node, parent = scan.parent; parent; scan = parent, parent = scan.parent) {
      if (scan instanceof TreeNode && scan.index < 0 && ((_a2 = parent.enter(pos, side, mode)) === null || _a2 === void 0 ? void 0 : _a2.from) != scan.from)
        node = parent;
    }
  for (; ; ) {
    let inner = node.enter(pos, side, mode);
    if (!inner)
      return node;
    node = inner;
  }
}
var BaseNode = class {
  cursor(mode = 0) {
    return new TreeCursor(this, mode);
  }
  getChild(type, before = null, after = null) {
    let r = getChildren(this, type, before, after);
    return r.length ? r[0] : null;
  }
  getChildren(type, before = null, after = null) {
    return getChildren(this, type, before, after);
  }
  resolve(pos, side = 0) {
    return resolveNode(this, pos, side, false);
  }
  resolveInner(pos, side = 0) {
    return resolveNode(this, pos, side, true);
  }
  matchContext(context) {
    return matchNodeContext(this.parent, context);
  }
  enterUnfinishedNodesBefore(pos) {
    let scan = this.childBefore(pos), node = this;
    while (scan) {
      let last = scan.lastChild;
      if (!last || last.to != scan.to)
        break;
      if (last.type.isError && last.from == last.to) {
        node = scan;
        scan = last.prevSibling;
      } else {
        scan = last;
      }
    }
    return node;
  }
  get node() {
    return this;
  }
  get next() {
    return this.parent;
  }
};
var TreeNode = class _TreeNode extends BaseNode {
  constructor(_tree, from, index, _parent) {
    super();
    this._tree = _tree;
    this.from = from;
    this.index = index;
    this._parent = _parent;
  }
  get type() {
    return this._tree.type;
  }
  get name() {
    return this._tree.type.name;
  }
  get to() {
    return this.from + this._tree.length;
  }
  nextChild(i, dir, pos, side, mode = 0) {
    for (let parent = this; ; ) {
      for (let { children, positions } = parent._tree, e = dir > 0 ? children.length : -1; i != e; i += dir) {
        let next = children[i], start = positions[i] + parent.from;
        if (!checkSide(side, pos, start, start + next.length))
          continue;
        if (next instanceof TreeBuffer) {
          if (mode & IterMode.ExcludeBuffers)
            continue;
          let index = next.findChild(0, next.buffer.length, dir, pos - start, side);
          if (index > -1)
            return new BufferNode(new BufferContext(parent, next, i, start), null, index);
        } else if (mode & IterMode.IncludeAnonymous || (!next.type.isAnonymous || hasChild(next))) {
          let mounted;
          if (!(mode & IterMode.IgnoreMounts) && (mounted = MountedTree.get(next)) && !mounted.overlay)
            return new _TreeNode(mounted.tree, start, i, parent);
          let inner = new _TreeNode(next, start, i, parent);
          return mode & IterMode.IncludeAnonymous || !inner.type.isAnonymous ? inner : inner.nextChild(dir < 0 ? next.children.length - 1 : 0, dir, pos, side);
        }
      }
      if (mode & IterMode.IncludeAnonymous || !parent.type.isAnonymous)
        return null;
      if (parent.index >= 0)
        i = parent.index + dir;
      else
        i = dir < 0 ? -1 : parent._parent._tree.children.length;
      parent = parent._parent;
      if (!parent)
        return null;
    }
  }
  get firstChild() {
    return this.nextChild(
      0,
      1,
      0,
      4
      /* Side.DontCare */
    );
  }
  get lastChild() {
    return this.nextChild(
      this._tree.children.length - 1,
      -1,
      0,
      4
      /* Side.DontCare */
    );
  }
  childAfter(pos) {
    return this.nextChild(
      0,
      1,
      pos,
      2
      /* Side.After */
    );
  }
  childBefore(pos) {
    return this.nextChild(
      this._tree.children.length - 1,
      -1,
      pos,
      -2
      /* Side.Before */
    );
  }
  prop(prop) {
    return this._tree.prop(prop);
  }
  enter(pos, side, mode = 0) {
    let mounted;
    if (!(mode & IterMode.IgnoreOverlays) && (mounted = MountedTree.get(this._tree)) && mounted.overlay) {
      let rPos = pos - this.from;
      for (let { from, to } of mounted.overlay) {
        if ((side > 0 ? from <= rPos : from < rPos) && (side < 0 ? to >= rPos : to > rPos))
          return new _TreeNode(mounted.tree, mounted.overlay[0].from + this.from, -1, this);
      }
    }
    return this.nextChild(0, 1, pos, side, mode);
  }
  nextSignificantParent() {
    let val = this;
    while (val.type.isAnonymous && val._parent)
      val = val._parent;
    return val;
  }
  get parent() {
    return this._parent ? this._parent.nextSignificantParent() : null;
  }
  get nextSibling() {
    return this._parent && this.index >= 0 ? this._parent.nextChild(
      this.index + 1,
      1,
      0,
      4
      /* Side.DontCare */
    ) : null;
  }
  get prevSibling() {
    return this._parent && this.index >= 0 ? this._parent.nextChild(
      this.index - 1,
      -1,
      0,
      4
      /* Side.DontCare */
    ) : null;
  }
  get tree() {
    return this._tree;
  }
  toTree() {
    return this._tree;
  }
  /**
  @internal
  */
  toString() {
    return this._tree.toString();
  }
};
function getChildren(node, type, before, after) {
  let cur = node.cursor(), result = [];
  if (!cur.firstChild())
    return result;
  if (before != null)
    for (let found = false; !found; ) {
      found = cur.type.is(before);
      if (!cur.nextSibling())
        return result;
    }
  for (; ; ) {
    if (after != null && cur.type.is(after))
      return result;
    if (cur.type.is(type))
      result.push(cur.node);
    if (!cur.nextSibling())
      return after == null ? result : [];
  }
}
function matchNodeContext(node, context, i = context.length - 1) {
  for (let p = node; i >= 0; p = p.parent) {
    if (!p)
      return false;
    if (!p.type.isAnonymous) {
      if (context[i] && context[i] != p.name)
        return false;
      i--;
    }
  }
  return true;
}
var BufferContext = class {
  constructor(parent, buffer, index, start) {
    this.parent = parent;
    this.buffer = buffer;
    this.index = index;
    this.start = start;
  }
};
var BufferNode = class _BufferNode extends BaseNode {
  get name() {
    return this.type.name;
  }
  get from() {
    return this.context.start + this.context.buffer.buffer[this.index + 1];
  }
  get to() {
    return this.context.start + this.context.buffer.buffer[this.index + 2];
  }
  constructor(context, _parent, index) {
    super();
    this.context = context;
    this._parent = _parent;
    this.index = index;
    this.type = context.buffer.set.types[context.buffer.buffer[index]];
  }
  child(dir, pos, side) {
    let { buffer } = this.context;
    let index = buffer.findChild(this.index + 4, buffer.buffer[this.index + 3], dir, pos - this.context.start, side);
    return index < 0 ? null : new _BufferNode(this.context, this, index);
  }
  get firstChild() {
    return this.child(
      1,
      0,
      4
      /* Side.DontCare */
    );
  }
  get lastChild() {
    return this.child(
      -1,
      0,
      4
      /* Side.DontCare */
    );
  }
  childAfter(pos) {
    return this.child(
      1,
      pos,
      2
      /* Side.After */
    );
  }
  childBefore(pos) {
    return this.child(
      -1,
      pos,
      -2
      /* Side.Before */
    );
  }
  prop(prop) {
    return this.type.prop(prop);
  }
  enter(pos, side, mode = 0) {
    if (mode & IterMode.ExcludeBuffers)
      return null;
    let { buffer } = this.context;
    let index = buffer.findChild(this.index + 4, buffer.buffer[this.index + 3], side > 0 ? 1 : -1, pos - this.context.start, side);
    return index < 0 ? null : new _BufferNode(this.context, this, index);
  }
  get parent() {
    return this._parent || this.context.parent.nextSignificantParent();
  }
  externalSibling(dir) {
    return this._parent ? null : this.context.parent.nextChild(
      this.context.index + dir,
      dir,
      0,
      4
      /* Side.DontCare */
    );
  }
  get nextSibling() {
    let { buffer } = this.context;
    let after = buffer.buffer[this.index + 3];
    if (after < (this._parent ? buffer.buffer[this._parent.index + 3] : buffer.buffer.length))
      return new _BufferNode(this.context, this._parent, after);
    return this.externalSibling(1);
  }
  get prevSibling() {
    let { buffer } = this.context;
    let parentStart = this._parent ? this._parent.index + 4 : 0;
    if (this.index == parentStart)
      return this.externalSibling(-1);
    return new _BufferNode(this.context, this._parent, buffer.findChild(
      parentStart,
      this.index,
      -1,
      0,
      4
      /* Side.DontCare */
    ));
  }
  get tree() {
    return null;
  }
  toTree() {
    let children = [], positions = [];
    let { buffer } = this.context;
    let startI = this.index + 4, endI = buffer.buffer[this.index + 3];
    if (endI > startI) {
      let from = buffer.buffer[this.index + 1];
      children.push(buffer.slice(startI, endI, from));
      positions.push(0);
    }
    return new Tree(this.type, children, positions, this.to - this.from);
  }
  /**
  @internal
  */
  toString() {
    return this.context.buffer.childString(this.index);
  }
};
function iterStack(heads) {
  if (!heads.length)
    return null;
  let pick = 0, picked = heads[0];
  for (let i = 1; i < heads.length; i++) {
    let node = heads[i];
    if (node.from > picked.from || node.to < picked.to) {
      picked = node;
      pick = i;
    }
  }
  let next = picked instanceof TreeNode && picked.index < 0 ? null : picked.parent;
  let newHeads = heads.slice();
  if (next)
    newHeads[pick] = next;
  else
    newHeads.splice(pick, 1);
  return new StackIterator(newHeads, picked);
}
var StackIterator = class {
  constructor(heads, node) {
    this.heads = heads;
    this.node = node;
  }
  get next() {
    return iterStack(this.heads);
  }
};
function stackIterator(tree, pos, side) {
  let inner = tree.resolveInner(pos, side), layers = null;
  for (let scan = inner instanceof TreeNode ? inner : inner.context.parent; scan; scan = scan.parent) {
    if (scan.index < 0) {
      let parent = scan.parent;
      (layers || (layers = [inner])).push(parent.resolve(pos, side));
      scan = parent;
    } else {
      let mount = MountedTree.get(scan.tree);
      if (mount && mount.overlay && mount.overlay[0].from <= pos && mount.overlay[mount.overlay.length - 1].to >= pos) {
        let root = new TreeNode(mount.tree, mount.overlay[0].from + scan.from, -1, scan);
        (layers || (layers = [inner])).push(resolveNode(root, pos, side, false));
      }
    }
  }
  return layers ? iterStack(layers) : inner;
}
var TreeCursor = class {
  /**
  Shorthand for `.type.name`.
  */
  get name() {
    return this.type.name;
  }
  /**
  @internal
  */
  constructor(node, mode = 0) {
    this.mode = mode;
    this.buffer = null;
    this.stack = [];
    this.index = 0;
    this.bufferNode = null;
    if (node instanceof TreeNode) {
      this.yieldNode(node);
    } else {
      this._tree = node.context.parent;
      this.buffer = node.context;
      for (let n = node._parent; n; n = n._parent)
        this.stack.unshift(n.index);
      this.bufferNode = node;
      this.yieldBuf(node.index);
    }
  }
  yieldNode(node) {
    if (!node)
      return false;
    this._tree = node;
    this.type = node.type;
    this.from = node.from;
    this.to = node.to;
    return true;
  }
  yieldBuf(index, type) {
    this.index = index;
    let { start, buffer } = this.buffer;
    this.type = type || buffer.set.types[buffer.buffer[index]];
    this.from = start + buffer.buffer[index + 1];
    this.to = start + buffer.buffer[index + 2];
    return true;
  }
  /**
  @internal
  */
  yield(node) {
    if (!node)
      return false;
    if (node instanceof TreeNode) {
      this.buffer = null;
      return this.yieldNode(node);
    }
    this.buffer = node.context;
    return this.yieldBuf(node.index, node.type);
  }
  /**
  @internal
  */
  toString() {
    return this.buffer ? this.buffer.buffer.childString(this.index) : this._tree.toString();
  }
  /**
  @internal
  */
  enterChild(dir, pos, side) {
    if (!this.buffer)
      return this.yield(this._tree.nextChild(dir < 0 ? this._tree._tree.children.length - 1 : 0, dir, pos, side, this.mode));
    let { buffer } = this.buffer;
    let index = buffer.findChild(this.index + 4, buffer.buffer[this.index + 3], dir, pos - this.buffer.start, side);
    if (index < 0)
      return false;
    this.stack.push(this.index);
    return this.yieldBuf(index);
  }
  /**
  Move the cursor to this node's first child. When this returns
  false, the node has no child, and the cursor has not been moved.
  */
  firstChild() {
    return this.enterChild(
      1,
      0,
      4
      /* Side.DontCare */
    );
  }
  /**
  Move the cursor to this node's last child.
  */
  lastChild() {
    return this.enterChild(
      -1,
      0,
      4
      /* Side.DontCare */
    );
  }
  /**
  Move the cursor to the first child that ends after `pos`.
  */
  childAfter(pos) {
    return this.enterChild(
      1,
      pos,
      2
      /* Side.After */
    );
  }
  /**
  Move to the last child that starts before `pos`.
  */
  childBefore(pos) {
    return this.enterChild(
      -1,
      pos,
      -2
      /* Side.Before */
    );
  }
  /**
  Move the cursor to the child around `pos`. If side is -1 the
  child may end at that position, when 1 it may start there. This
  will also enter [overlaid](#common.MountedTree.overlay)
  [mounted](#common.NodeProp^mounted) trees unless `overlays` is
  set to false.
  */
  enter(pos, side, mode = this.mode) {
    if (!this.buffer)
      return this.yield(this._tree.enter(pos, side, mode));
    return mode & IterMode.ExcludeBuffers ? false : this.enterChild(1, pos, side);
  }
  /**
  Move to the node's parent node, if this isn't the top node.
  */
  parent() {
    if (!this.buffer)
      return this.yieldNode(this.mode & IterMode.IncludeAnonymous ? this._tree._parent : this._tree.parent);
    if (this.stack.length)
      return this.yieldBuf(this.stack.pop());
    let parent = this.mode & IterMode.IncludeAnonymous ? this.buffer.parent : this.buffer.parent.nextSignificantParent();
    this.buffer = null;
    return this.yieldNode(parent);
  }
  /**
  @internal
  */
  sibling(dir) {
    if (!this.buffer)
      return !this._tree._parent ? false : this.yield(this._tree.index < 0 ? null : this._tree._parent.nextChild(this._tree.index + dir, dir, 0, 4, this.mode));
    let { buffer } = this.buffer, d = this.stack.length - 1;
    if (dir < 0) {
      let parentStart = d < 0 ? 0 : this.stack[d] + 4;
      if (this.index != parentStart)
        return this.yieldBuf(buffer.findChild(
          parentStart,
          this.index,
          -1,
          0,
          4
          /* Side.DontCare */
        ));
    } else {
      let after = buffer.buffer[this.index + 3];
      if (after < (d < 0 ? buffer.buffer.length : buffer.buffer[this.stack[d] + 3]))
        return this.yieldBuf(after);
    }
    return d < 0 ? this.yield(this.buffer.parent.nextChild(this.buffer.index + dir, dir, 0, 4, this.mode)) : false;
  }
  /**
  Move to this node's next sibling, if any.
  */
  nextSibling() {
    return this.sibling(1);
  }
  /**
  Move to this node's previous sibling, if any.
  */
  prevSibling() {
    return this.sibling(-1);
  }
  atLastNode(dir) {
    let index, parent, { buffer } = this;
    if (buffer) {
      if (dir > 0) {
        if (this.index < buffer.buffer.buffer.length)
          return false;
      } else {
        for (let i = 0; i < this.index; i++)
          if (buffer.buffer.buffer[i + 3] < this.index)
            return false;
      }
      ({ index, parent } = buffer);
    } else {
      ({ index, _parent: parent } = this._tree);
    }
    for (; parent; { index, _parent: parent } = parent) {
      if (index > -1)
        for (let i = index + dir, e = dir < 0 ? -1 : parent._tree.children.length; i != e; i += dir) {
          let child = parent._tree.children[i];
          if (this.mode & IterMode.IncludeAnonymous || child instanceof TreeBuffer || !child.type.isAnonymous || hasChild(child))
            return false;
        }
    }
    return true;
  }
  move(dir, enter) {
    if (enter && this.enterChild(
      dir,
      0,
      4
      /* Side.DontCare */
    ))
      return true;
    for (; ; ) {
      if (this.sibling(dir))
        return true;
      if (this.atLastNode(dir) || !this.parent())
        return false;
    }
  }
  /**
  Move to the next node in a
  [pre-order](https://en.wikipedia.org/wiki/Tree_traversal#Pre-order,_NLR)
  traversal, going from a node to its first child or, if the
  current node is empty or `enter` is false, its next sibling or
  the next sibling of the first parent node that has one.
  */
  next(enter = true) {
    return this.move(1, enter);
  }
  /**
  Move to the next node in a last-to-first pre-order traversal. A
  node is followed by its last child or, if it has none, its
  previous sibling or the previous sibling of the first parent
  node that has one.
  */
  prev(enter = true) {
    return this.move(-1, enter);
  }
  /**
  Move the cursor to the innermost node that covers `pos`. If
  `side` is -1, it will enter nodes that end at `pos`. If it is 1,
  it will enter nodes that start at `pos`.
  */
  moveTo(pos, side = 0) {
    while (this.from == this.to || (side < 1 ? this.from >= pos : this.from > pos) || (side > -1 ? this.to <= pos : this.to < pos))
      if (!this.parent())
        break;
    while (this.enterChild(1, pos, side)) {
    }
    return this;
  }
  /**
  Get a [syntax node](#common.SyntaxNode) at the cursor's current
  position.
  */
  get node() {
    if (!this.buffer)
      return this._tree;
    let cache = this.bufferNode, result = null, depth = 0;
    if (cache && cache.context == this.buffer) {
      scan: for (let index = this.index, d = this.stack.length; d >= 0; ) {
        for (let c = cache; c; c = c._parent)
          if (c.index == index) {
            if (index == this.index)
              return c;
            result = c;
            depth = d + 1;
            break scan;
          }
        index = this.stack[--d];
      }
    }
    for (let i = depth; i < this.stack.length; i++)
      result = new BufferNode(this.buffer, result, this.stack[i]);
    return this.bufferNode = new BufferNode(this.buffer, result, this.index);
  }
  /**
  Get the [tree](#common.Tree) that represents the current node, if
  any. Will return null when the node is in a [tree
  buffer](#common.TreeBuffer).
  */
  get tree() {
    return this.buffer ? null : this._tree._tree;
  }
  /**
  Iterate over the current node and all its descendants, calling
  `enter` when entering a node and `leave`, if given, when leaving
  one. When `enter` returns `false`, any children of that node are
  skipped, and `leave` isn't called for it.
  */
  iterate(enter, leave) {
    for (let depth = 0; ; ) {
      let mustLeave = false;
      if (this.type.isAnonymous || enter(this) !== false) {
        if (this.firstChild()) {
          depth++;
          continue;
        }
        if (!this.type.isAnonymous)
          mustLeave = true;
      }
      for (; ; ) {
        if (mustLeave && leave)
          leave(this);
        mustLeave = this.type.isAnonymous;
        if (!depth)
          return;
        if (this.nextSibling())
          break;
        this.parent();
        depth--;
        mustLeave = true;
      }
    }
  }
  /**
  Test whether the current node matches a given context—a sequence
  of direct parent node names. Empty strings in the context array
  are treated as wildcards.
  */
  matchContext(context) {
    if (!this.buffer)
      return matchNodeContext(this.node.parent, context);
    let { buffer } = this.buffer, { types: types2 } = buffer.set;
    for (let i = context.length - 1, d = this.stack.length - 1; i >= 0; d--) {
      if (d < 0)
        return matchNodeContext(this._tree, context, i);
      let type = types2[buffer.buffer[this.stack[d]]];
      if (!type.isAnonymous) {
        if (context[i] && context[i] != type.name)
          return false;
        i--;
      }
    }
    return true;
  }
};
function hasChild(tree) {
  return tree.children.some((ch) => ch instanceof TreeBuffer || !ch.type.isAnonymous || hasChild(ch));
}
function buildTree(data) {
  var _a2;
  let { buffer, nodeSet, maxBufferLength = DefaultBufferLength, reused = [], minRepeatType = nodeSet.types.length } = data;
  let cursor = Array.isArray(buffer) ? new FlatBufferCursor(buffer, buffer.length) : buffer;
  let types2 = nodeSet.types;
  let contextHash = 0, lookAhead = 0;
  function takeNode(parentStart, minPos, children2, positions2, inRepeat, depth) {
    let { id, start, end, size } = cursor;
    let lookAheadAtStart = lookAhead, contextAtStart = contextHash;
    if (size < 0) {
      cursor.next();
      if (size == -1) {
        let node2 = reused[id];
        children2.push(node2);
        positions2.push(start - parentStart);
        return;
      } else if (size == -3) {
        contextHash = id;
        return;
      } else if (size == -4) {
        lookAhead = id;
        return;
      } else {
        throw new RangeError(`Unrecognized record size: ${size}`);
      }
    }
    let type = types2[id], node, buffer2;
    let startPos = start - parentStart;
    if (end - start <= maxBufferLength && (buffer2 = findBufferSize(cursor.pos - minPos, inRepeat))) {
      let data2 = new Uint16Array(buffer2.size - buffer2.skip);
      let endPos = cursor.pos - buffer2.size, index = data2.length;
      while (cursor.pos > endPos)
        index = copyToBuffer(buffer2.start, data2, index);
      node = new TreeBuffer(data2, end - buffer2.start, nodeSet);
      startPos = buffer2.start - parentStart;
    } else {
      let endPos = cursor.pos - size;
      cursor.next();
      let localChildren = [], localPositions = [];
      let localInRepeat = id >= minRepeatType ? id : -1;
      let lastGroup = 0, lastEnd = end;
      while (cursor.pos > endPos) {
        if (localInRepeat >= 0 && cursor.id == localInRepeat && cursor.size >= 0) {
          if (cursor.end <= lastEnd - maxBufferLength) {
            makeRepeatLeaf(localChildren, localPositions, start, lastGroup, cursor.end, lastEnd, localInRepeat, lookAheadAtStart, contextAtStart);
            lastGroup = localChildren.length;
            lastEnd = cursor.end;
          }
          cursor.next();
        } else if (depth > 2500) {
          takeFlatNode(start, endPos, localChildren, localPositions);
        } else {
          takeNode(start, endPos, localChildren, localPositions, localInRepeat, depth + 1);
        }
      }
      if (localInRepeat >= 0 && lastGroup > 0 && lastGroup < localChildren.length)
        makeRepeatLeaf(localChildren, localPositions, start, lastGroup, start, lastEnd, localInRepeat, lookAheadAtStart, contextAtStart);
      localChildren.reverse();
      localPositions.reverse();
      if (localInRepeat > -1 && lastGroup > 0) {
        let make = makeBalanced(type, contextAtStart);
        node = balanceRange(type, localChildren, localPositions, 0, localChildren.length, 0, end - start, make, make);
      } else {
        node = makeTree(type, localChildren, localPositions, end - start, lookAheadAtStart - end, contextAtStart);
      }
    }
    children2.push(node);
    positions2.push(startPos);
  }
  function takeFlatNode(parentStart, minPos, children2, positions2) {
    let nodes = [];
    let nodeCount = 0, stopAt = -1;
    while (cursor.pos > minPos) {
      let { id, start, end, size } = cursor;
      if (size > 4) {
        cursor.next();
      } else if (stopAt > -1 && start < stopAt) {
        break;
      } else {
        if (stopAt < 0)
          stopAt = end - maxBufferLength;
        nodes.push(id, start, end);
        nodeCount++;
        cursor.next();
      }
    }
    if (nodeCount) {
      let buffer2 = new Uint16Array(nodeCount * 4);
      let start = nodes[nodes.length - 2];
      for (let i = nodes.length - 3, j = 0; i >= 0; i -= 3) {
        buffer2[j++] = nodes[i];
        buffer2[j++] = nodes[i + 1] - start;
        buffer2[j++] = nodes[i + 2] - start;
        buffer2[j++] = j;
      }
      children2.push(new TreeBuffer(buffer2, nodes[2] - start, nodeSet));
      positions2.push(start - parentStart);
    }
  }
  function makeBalanced(type, contextHash2) {
    return (children2, positions2, length2) => {
      let lookAhead2 = 0, lastI = children2.length - 1, last, lookAheadProp;
      if (lastI >= 0 && (last = children2[lastI]) instanceof Tree) {
        if (!lastI && last.type == type && last.length == length2)
          return last;
        if (lookAheadProp = last.prop(NodeProp.lookAhead))
          lookAhead2 = positions2[lastI] + last.length + lookAheadProp;
      }
      return makeTree(type, children2, positions2, length2, lookAhead2, contextHash2);
    };
  }
  function makeRepeatLeaf(children2, positions2, base2, i, from, to, type, lookAhead2, contextHash2) {
    let localChildren = [], localPositions = [];
    while (children2.length > i) {
      localChildren.push(children2.pop());
      localPositions.push(positions2.pop() + base2 - from);
    }
    children2.push(makeTree(nodeSet.types[type], localChildren, localPositions, to - from, lookAhead2 - to, contextHash2));
    positions2.push(from - base2);
  }
  function makeTree(type, children2, positions2, length2, lookAhead2, contextHash2, props) {
    if (contextHash2) {
      let pair2 = [NodeProp.contextHash, contextHash2];
      props = props ? [pair2].concat(props) : [pair2];
    }
    if (lookAhead2 > 25) {
      let pair2 = [NodeProp.lookAhead, lookAhead2];
      props = props ? [pair2].concat(props) : [pair2];
    }
    return new Tree(type, children2, positions2, length2, props);
  }
  function findBufferSize(maxSize, inRepeat) {
    let fork = cursor.fork();
    let size = 0, start = 0, skip = 0, minStart = fork.end - maxBufferLength;
    let result = { size: 0, start: 0, skip: 0 };
    scan: for (let minPos = fork.pos - maxSize; fork.pos > minPos; ) {
      let nodeSize2 = fork.size;
      if (fork.id == inRepeat && nodeSize2 >= 0) {
        result.size = size;
        result.start = start;
        result.skip = skip;
        skip += 4;
        size += 4;
        fork.next();
        continue;
      }
      let startPos = fork.pos - nodeSize2;
      if (nodeSize2 < 0 || startPos < minPos || fork.start < minStart)
        break;
      let localSkipped = fork.id >= minRepeatType ? 4 : 0;
      let nodeStart = fork.start;
      fork.next();
      while (fork.pos > startPos) {
        if (fork.size < 0) {
          if (fork.size == -3 || fork.size == -4)
            localSkipped += 4;
          else
            break scan;
        } else if (fork.id >= minRepeatType) {
          localSkipped += 4;
        }
        fork.next();
      }
      start = nodeStart;
      size += nodeSize2;
      skip += localSkipped;
    }
    if (inRepeat < 0 || size == maxSize) {
      result.size = size;
      result.start = start;
      result.skip = skip;
    }
    return result.size > 4 ? result : void 0;
  }
  function copyToBuffer(bufferStart, buffer2, index) {
    let { id, start, end, size } = cursor;
    cursor.next();
    if (size >= 0 && id < minRepeatType) {
      let startIndex = index;
      if (size > 4) {
        let endPos = cursor.pos - (size - 4);
        while (cursor.pos > endPos)
          index = copyToBuffer(bufferStart, buffer2, index);
      }
      buffer2[--index] = startIndex;
      buffer2[--index] = end - bufferStart;
      buffer2[--index] = start - bufferStart;
      buffer2[--index] = id;
    } else if (size == -3) {
      contextHash = id;
    } else if (size == -4) {
      lookAhead = id;
    }
    return index;
  }
  let children = [], positions = [];
  while (cursor.pos > 0)
    takeNode(data.start || 0, data.bufferStart || 0, children, positions, -1, 0);
  let length = (_a2 = data.length) !== null && _a2 !== void 0 ? _a2 : children.length ? positions[0] + children[0].length : 0;
  return new Tree(types2[data.topID], children.reverse(), positions.reverse(), length);
}
var nodeSizeCache = /* @__PURE__ */ new WeakMap();
function nodeSize(balanceType, node) {
  if (!balanceType.isAnonymous || node instanceof TreeBuffer || node.type != balanceType)
    return 1;
  let size = nodeSizeCache.get(node);
  if (size == null) {
    size = 1;
    for (let child of node.children) {
      if (child.type != balanceType || !(child instanceof Tree)) {
        size = 1;
        break;
      }
      size += nodeSize(balanceType, child);
    }
    nodeSizeCache.set(node, size);
  }
  return size;
}
function balanceRange(balanceType, children, positions, from, to, start, length, mkTop, mkTree) {
  let total = 0;
  for (let i = from; i < to; i++)
    total += nodeSize(balanceType, children[i]);
  let maxChild = Math.ceil(
    total * 1.5 / 8
    /* Balance.BranchFactor */
  );
  let localChildren = [], localPositions = [];
  function divide(children2, positions2, from2, to2, offset) {
    for (let i = from2; i < to2; ) {
      let groupFrom = i, groupStart = positions2[i], groupSize = nodeSize(balanceType, children2[i]);
      i++;
      for (; i < to2; i++) {
        let nextSize = nodeSize(balanceType, children2[i]);
        if (groupSize + nextSize >= maxChild)
          break;
        groupSize += nextSize;
      }
      if (i == groupFrom + 1) {
        if (groupSize > maxChild) {
          let only = children2[groupFrom];
          divide(only.children, only.positions, 0, only.children.length, positions2[groupFrom] + offset);
          continue;
        }
        localChildren.push(children2[groupFrom]);
      } else {
        let length2 = positions2[i - 1] + children2[i - 1].length - groupStart;
        localChildren.push(balanceRange(balanceType, children2, positions2, groupFrom, i, groupStart, length2, null, mkTree));
      }
      localPositions.push(groupStart + offset - start);
    }
  }
  divide(children, positions, from, to, 0);
  return (mkTop || mkTree)(localChildren, localPositions, length);
}
var TreeFragment = class _TreeFragment {
  /**
  Construct a tree fragment. You'll usually want to use
  [`addTree`](#common.TreeFragment^addTree) and
  [`applyChanges`](#common.TreeFragment^applyChanges) instead of
  calling this directly.
  */
  constructor(from, to, tree, offset, openStart = false, openEnd = false) {
    this.from = from;
    this.to = to;
    this.tree = tree;
    this.offset = offset;
    this.open = (openStart ? 1 : 0) | (openEnd ? 2 : 0);
  }
  /**
  Whether the start of the fragment represents the start of a
  parse, or the end of a change. (In the second case, it may not
  be safe to reuse some nodes at the start, depending on the
  parsing algorithm.)
  */
  get openStart() {
    return (this.open & 1) > 0;
  }
  /**
  Whether the end of the fragment represents the end of a
  full-document parse, or the start of a change.
  */
  get openEnd() {
    return (this.open & 2) > 0;
  }
  /**
  Create a set of fragments from a freshly parsed tree, or update
  an existing set of fragments by replacing the ones that overlap
  with a tree with content from the new tree. When `partial` is
  true, the parse is treated as incomplete, and the resulting
  fragment has [`openEnd`](#common.TreeFragment.openEnd) set to
  true.
  */
  static addTree(tree, fragments = [], partial = false) {
    let result = [new _TreeFragment(0, tree.length, tree, 0, false, partial)];
    for (let f of fragments)
      if (f.to > tree.length)
        result.push(f);
    return result;
  }
  /**
  Apply a set of edits to an array of fragments, removing or
  splitting fragments as necessary to remove edited ranges, and
  adjusting offsets for fragments that moved.
  */
  static applyChanges(fragments, changes, minGap = 128) {
    if (!changes.length)
      return fragments;
    let result = [];
    let fI = 1, nextF = fragments.length ? fragments[0] : null;
    for (let cI = 0, pos = 0, off = 0; ; cI++) {
      let nextC = cI < changes.length ? changes[cI] : null;
      let nextPos = nextC ? nextC.fromA : 1e9;
      if (nextPos - pos >= minGap)
        while (nextF && nextF.from < nextPos) {
          let cut = nextF;
          if (pos >= cut.from || nextPos <= cut.to || off) {
            let fFrom = Math.max(cut.from, pos) - off, fTo = Math.min(cut.to, nextPos) - off;
            cut = fFrom >= fTo ? null : new _TreeFragment(fFrom, fTo, cut.tree, cut.offset + off, cI > 0, !!nextC);
          }
          if (cut)
            result.push(cut);
          if (nextF.to > nextPos)
            break;
          nextF = fI < fragments.length ? fragments[fI++] : null;
        }
      if (!nextC)
        break;
      pos = nextC.toA;
      off = nextC.toA - nextC.toB;
    }
    return result;
  }
};
var Parser = class {
  /**
  Start a parse, returning a [partial parse](#common.PartialParse)
  object. [`fragments`](#common.TreeFragment) can be passed in to
  make the parse incremental.
  
  By default, the entire input is parsed. You can pass `ranges`,
  which should be a sorted array of non-empty, non-overlapping
  ranges, to parse only those ranges. The tree returned in that
  case will start at `ranges[0].from`.
  */
  startParse(input, fragments, ranges) {
    if (typeof input == "string")
      input = new StringInput(input);
    ranges = !ranges ? [new Range(0, input.length)] : ranges.length ? ranges.map((r) => new Range(r.from, r.to)) : [new Range(0, 0)];
    return this.createParse(input, fragments || [], ranges);
  }
  /**
  Run a full parse, returning the resulting tree.
  */
  parse(input, fragments, ranges) {
    let parse = this.startParse(input, fragments, ranges);
    for (; ; ) {
      let done = parse.advance();
      if (done)
        return done;
    }
  }
};
var StringInput = class {
  constructor(string2) {
    this.string = string2;
  }
  get length() {
    return this.string.length;
  }
  chunk(from) {
    return this.string.slice(from);
  }
  get lineChunks() {
    return false;
  }
  read(from, to) {
    return this.string.slice(from, to);
  }
};
var stoppedInner = new NodeProp({ perNode: true });

// node_modules/@lezer/lr/dist/index.js
var Stack = class _Stack {
  /**
  @internal
  */
  constructor(p, stack, state, reducePos, pos, score, buffer, bufferBase, curContext, lookAhead = 0, parent) {
    this.p = p;
    this.stack = stack;
    this.state = state;
    this.reducePos = reducePos;
    this.pos = pos;
    this.score = score;
    this.buffer = buffer;
    this.bufferBase = bufferBase;
    this.curContext = curContext;
    this.lookAhead = lookAhead;
    this.parent = parent;
  }
  /**
  @internal
  */
  toString() {
    return `[${this.stack.filter((_, i) => i % 3 == 0).concat(this.state)}]@${this.pos}${this.score ? "!" + this.score : ""}`;
  }
  // Start an empty stack
  /**
  @internal
  */
  static start(p, state, pos = 0) {
    let cx = p.parser.context;
    return new _Stack(p, [], state, pos, pos, 0, [], 0, cx ? new StackContext(cx, cx.start) : null, 0, null);
  }
  /**
  The stack's current [context](#lr.ContextTracker) value, if
  any. Its type will depend on the context tracker's type
  parameter, or it will be `null` if there is no context
  tracker.
  */
  get context() {
    return this.curContext ? this.curContext.context : null;
  }
  // Push a state onto the stack, tracking its start position as well
  // as the buffer base at that point.
  /**
  @internal
  */
  pushState(state, start) {
    this.stack.push(this.state, start, this.bufferBase + this.buffer.length);
    this.state = state;
  }
  // Apply a reduce action
  /**
  @internal
  */
  reduce(action) {
    var _a2;
    let depth = action >> 19, type = action & 65535;
    let { parser: parser2 } = this.p;
    let lookaheadRecord = this.reducePos < this.pos - 25 && this.setLookAhead(this.pos);
    let dPrec = parser2.dynamicPrecedence(type);
    if (dPrec)
      this.score += dPrec;
    if (depth == 0) {
      this.pushState(parser2.getGoto(this.state, type, true), this.reducePos);
      if (type < parser2.minRepeatTerm)
        this.storeNode(type, this.reducePos, this.reducePos, lookaheadRecord ? 8 : 4, true);
      this.reduceContext(type, this.reducePos);
      return;
    }
    let base2 = this.stack.length - (depth - 1) * 3 - (action & 262144 ? 6 : 0);
    let start = base2 ? this.stack[base2 - 2] : this.p.ranges[0].from, size = this.reducePos - start;
    if (size >= 2e3 && !((_a2 = this.p.parser.nodeSet.types[type]) === null || _a2 === void 0 ? void 0 : _a2.isAnonymous)) {
      if (start == this.p.lastBigReductionStart) {
        this.p.bigReductionCount++;
        this.p.lastBigReductionSize = size;
      } else if (this.p.lastBigReductionSize < size) {
        this.p.bigReductionCount = 1;
        this.p.lastBigReductionStart = start;
        this.p.lastBigReductionSize = size;
      }
    }
    let bufferBase = base2 ? this.stack[base2 - 1] : 0, count = this.bufferBase + this.buffer.length - bufferBase;
    if (type < parser2.minRepeatTerm || action & 131072) {
      let pos = parser2.stateFlag(
        this.state,
        1
        /* StateFlag.Skipped */
      ) ? this.pos : this.reducePos;
      this.storeNode(type, start, pos, count + 4, true);
    }
    if (action & 262144) {
      this.state = this.stack[base2];
    } else {
      let baseStateID = this.stack[base2 - 3];
      this.state = parser2.getGoto(baseStateID, type, true);
    }
    while (this.stack.length > base2)
      this.stack.pop();
    this.reduceContext(type, start);
  }
  // Shift a value into the buffer
  /**
  @internal
  */
  storeNode(term, start, end, size = 4, mustSink = false) {
    if (term == 0 && (!this.stack.length || this.stack[this.stack.length - 1] < this.buffer.length + this.bufferBase)) {
      let cur = this, top2 = this.buffer.length;
      if (top2 == 0 && cur.parent) {
        top2 = cur.bufferBase - cur.parent.bufferBase;
        cur = cur.parent;
      }
      if (top2 > 0 && cur.buffer[top2 - 4] == 0 && cur.buffer[top2 - 1] > -1) {
        if (start == end)
          return;
        if (cur.buffer[top2 - 2] >= start) {
          cur.buffer[top2 - 2] = end;
          return;
        }
      }
    }
    if (!mustSink || this.pos == end) {
      this.buffer.push(term, start, end, size);
    } else {
      let index = this.buffer.length;
      if (index > 0 && (this.buffer[index - 4] != 0 || this.buffer[index - 1] < 0)) {
        let mustMove = false;
        for (let scan = index; scan > 0 && this.buffer[scan - 2] > end; scan -= 4) {
          if (this.buffer[scan - 1] >= 0) {
            mustMove = true;
            break;
          }
        }
        if (mustMove)
          while (index > 0 && this.buffer[index - 2] > end) {
            this.buffer[index] = this.buffer[index - 4];
            this.buffer[index + 1] = this.buffer[index - 3];
            this.buffer[index + 2] = this.buffer[index - 2];
            this.buffer[index + 3] = this.buffer[index - 1];
            index -= 4;
            if (size > 4)
              size -= 4;
          }
      }
      this.buffer[index] = term;
      this.buffer[index + 1] = start;
      this.buffer[index + 2] = end;
      this.buffer[index + 3] = size;
    }
  }
  // Apply a shift action
  /**
  @internal
  */
  shift(action, type, start, end) {
    if (action & 131072) {
      this.pushState(action & 65535, this.pos);
    } else if ((action & 262144) == 0) {
      let nextState = action, { parser: parser2 } = this.p;
      if (end > this.pos || type <= parser2.maxNode) {
        this.pos = end;
        if (!parser2.stateFlag(
          nextState,
          1
          /* StateFlag.Skipped */
        ))
          this.reducePos = end;
      }
      this.pushState(nextState, start);
      this.shiftContext(type, start);
      if (type <= parser2.maxNode)
        this.buffer.push(type, start, end, 4);
    } else {
      this.pos = end;
      this.shiftContext(type, start);
      if (type <= this.p.parser.maxNode)
        this.buffer.push(type, start, end, 4);
    }
  }
  // Apply an action
  /**
  @internal
  */
  apply(action, next, nextStart, nextEnd) {
    if (action & 65536)
      this.reduce(action);
    else
      this.shift(action, next, nextStart, nextEnd);
  }
  // Add a prebuilt (reused) node into the buffer.
  /**
  @internal
  */
  useNode(value, next) {
    let index = this.p.reused.length - 1;
    if (index < 0 || this.p.reused[index] != value) {
      this.p.reused.push(value);
      index++;
    }
    let start = this.pos;
    this.reducePos = this.pos = start + value.length;
    this.pushState(next, start);
    this.buffer.push(
      index,
      start,
      this.reducePos,
      -1
      /* size == -1 means this is a reused value */
    );
    if (this.curContext)
      this.updateContext(this.curContext.tracker.reuse(this.curContext.context, value, this, this.p.stream.reset(this.pos - value.length)));
  }
  // Split the stack. Due to the buffer sharing and the fact
  // that `this.stack` tends to stay quite shallow, this isn't very
  // expensive.
  /**
  @internal
  */
  split() {
    let parent = this;
    let off = parent.buffer.length;
    while (off > 0 && parent.buffer[off - 2] > parent.reducePos)
      off -= 4;
    let buffer = parent.buffer.slice(off), base2 = parent.bufferBase + off;
    while (parent && base2 == parent.bufferBase)
      parent = parent.parent;
    return new _Stack(this.p, this.stack.slice(), this.state, this.reducePos, this.pos, this.score, buffer, base2, this.curContext, this.lookAhead, parent);
  }
  // Try to recover from an error by 'deleting' (ignoring) one token.
  /**
  @internal
  */
  recoverByDelete(next, nextEnd) {
    let isNode = next <= this.p.parser.maxNode;
    if (isNode)
      this.storeNode(next, this.pos, nextEnd, 4);
    this.storeNode(0, this.pos, nextEnd, isNode ? 8 : 4);
    this.pos = this.reducePos = nextEnd;
    this.score -= 190;
  }
  /**
  Check if the given term would be able to be shifted (optionally
  after some reductions) on this stack. This can be useful for
  external tokenizers that want to make sure they only provide a
  given token when it applies.
  */
  canShift(term) {
    for (let sim = new SimulatedStack(this); ; ) {
      let action = this.p.parser.stateSlot(
        sim.state,
        4
        /* ParseState.DefaultReduce */
      ) || this.p.parser.hasAction(sim.state, term);
      if (action == 0)
        return false;
      if ((action & 65536) == 0)
        return true;
      sim.reduce(action);
    }
  }
  // Apply up to Recover.MaxNext recovery actions that conceptually
  // inserts some missing token or rule.
  /**
  @internal
  */
  recoverByInsert(next) {
    if (this.stack.length >= 300)
      return [];
    let nextStates = this.p.parser.nextStates(this.state);
    if (nextStates.length > 4 << 1 || this.stack.length >= 120) {
      let best = [];
      for (let i = 0, s; i < nextStates.length; i += 2) {
        if ((s = nextStates[i + 1]) != this.state && this.p.parser.hasAction(s, next))
          best.push(nextStates[i], s);
      }
      if (this.stack.length < 120)
        for (let i = 0; best.length < 4 << 1 && i < nextStates.length; i += 2) {
          let s = nextStates[i + 1];
          if (!best.some((v, i2) => i2 & 1 && v == s))
            best.push(nextStates[i], s);
        }
      nextStates = best;
    }
    let result = [];
    for (let i = 0; i < nextStates.length && result.length < 4; i += 2) {
      let s = nextStates[i + 1];
      if (s == this.state)
        continue;
      let stack = this.split();
      stack.pushState(s, this.pos);
      stack.storeNode(0, stack.pos, stack.pos, 4, true);
      stack.shiftContext(nextStates[i], this.pos);
      stack.reducePos = this.pos;
      stack.score -= 200;
      result.push(stack);
    }
    return result;
  }
  // Force a reduce, if possible. Return false if that can't
  // be done.
  /**
  @internal
  */
  forceReduce() {
    let { parser: parser2 } = this.p;
    let reduce = parser2.stateSlot(
      this.state,
      5
      /* ParseState.ForcedReduce */
    );
    if ((reduce & 65536) == 0)
      return false;
    if (!parser2.validAction(this.state, reduce)) {
      let depth = reduce >> 19, term = reduce & 65535;
      let target = this.stack.length - depth * 3;
      if (target < 0 || parser2.getGoto(this.stack[target], term, false) < 0) {
        let backup = this.findForcedReduction();
        if (backup == null)
          return false;
        reduce = backup;
      }
      this.storeNode(0, this.pos, this.pos, 4, true);
      this.score -= 100;
    }
    this.reducePos = this.pos;
    this.reduce(reduce);
    return true;
  }
  /**
  Try to scan through the automaton to find some kind of reduction
  that can be applied. Used when the regular ForcedReduce field
  isn't a valid action. @internal
  */
  findForcedReduction() {
    let { parser: parser2 } = this.p, seen = [];
    let explore = (state, depth) => {
      if (seen.includes(state))
        return;
      seen.push(state);
      return parser2.allActions(state, (action) => {
        if (action & (262144 | 131072)) ;
        else if (action & 65536) {
          let rDepth = (action >> 19) - depth;
          if (rDepth > 1) {
            let term = action & 65535, target = this.stack.length - rDepth * 3;
            if (target >= 0 && parser2.getGoto(this.stack[target], term, false) >= 0)
              return rDepth << 19 | 65536 | term;
          }
        } else {
          let found = explore(action, depth + 1);
          if (found != null)
            return found;
        }
      });
    };
    return explore(this.state, 0);
  }
  /**
  @internal
  */
  forceAll() {
    while (!this.p.parser.stateFlag(
      this.state,
      2
      /* StateFlag.Accepting */
    )) {
      if (!this.forceReduce()) {
        this.storeNode(0, this.pos, this.pos, 4, true);
        break;
      }
    }
    return this;
  }
  /**
  Check whether this state has no further actions (assumed to be a direct descendant of the
  top state, since any other states must be able to continue
  somehow). @internal
  */
  get deadEnd() {
    if (this.stack.length != 3)
      return false;
    let { parser: parser2 } = this.p;
    return parser2.data[parser2.stateSlot(
      this.state,
      1
      /* ParseState.Actions */
    )] == 65535 && !parser2.stateSlot(
      this.state,
      4
      /* ParseState.DefaultReduce */
    );
  }
  /**
  Restart the stack (put it back in its start state). Only safe
  when this.stack.length == 3 (state is directly below the top
  state). @internal
  */
  restart() {
    this.storeNode(0, this.pos, this.pos, 4, true);
    this.state = this.stack[0];
    this.stack.length = 0;
  }
  /**
  @internal
  */
  sameState(other) {
    if (this.state != other.state || this.stack.length != other.stack.length)
      return false;
    for (let i = 0; i < this.stack.length; i += 3)
      if (this.stack[i] != other.stack[i])
        return false;
    return true;
  }
  /**
  Get the parser used by this stack.
  */
  get parser() {
    return this.p.parser;
  }
  /**
  Test whether a given dialect (by numeric ID, as exported from
  the terms file) is enabled.
  */
  dialectEnabled(dialectID) {
    return this.p.parser.dialect.flags[dialectID];
  }
  shiftContext(term, start) {
    if (this.curContext)
      this.updateContext(this.curContext.tracker.shift(this.curContext.context, term, this, this.p.stream.reset(start)));
  }
  reduceContext(term, start) {
    if (this.curContext)
      this.updateContext(this.curContext.tracker.reduce(this.curContext.context, term, this, this.p.stream.reset(start)));
  }
  /**
  @internal
  */
  emitContext() {
    let last = this.buffer.length - 1;
    if (last < 0 || this.buffer[last] != -3)
      this.buffer.push(this.curContext.hash, this.pos, this.pos, -3);
  }
  /**
  @internal
  */
  emitLookAhead() {
    let last = this.buffer.length - 1;
    if (last < 0 || this.buffer[last] != -4)
      this.buffer.push(this.lookAhead, this.pos, this.pos, -4);
  }
  updateContext(context) {
    if (context != this.curContext.context) {
      let newCx = new StackContext(this.curContext.tracker, context);
      if (newCx.hash != this.curContext.hash)
        this.emitContext();
      this.curContext = newCx;
    }
  }
  /**
  @internal
  */
  setLookAhead(lookAhead) {
    if (lookAhead <= this.lookAhead)
      return false;
    this.emitLookAhead();
    this.lookAhead = lookAhead;
    return true;
  }
  /**
  @internal
  */
  close() {
    if (this.curContext && this.curContext.tracker.strict)
      this.emitContext();
    if (this.lookAhead > 0)
      this.emitLookAhead();
  }
};
var StackContext = class {
  constructor(tracker, context) {
    this.tracker = tracker;
    this.context = context;
    this.hash = tracker.strict ? tracker.hash(context) : 0;
  }
};
var SimulatedStack = class {
  constructor(start) {
    this.start = start;
    this.state = start.state;
    this.stack = start.stack;
    this.base = this.stack.length;
  }
  reduce(action) {
    let term = action & 65535, depth = action >> 19;
    if (depth == 0) {
      if (this.stack == this.start.stack)
        this.stack = this.stack.slice();
      this.stack.push(this.state, 0, 0);
      this.base += 3;
    } else {
      this.base -= (depth - 1) * 3;
    }
    let goto = this.start.p.parser.getGoto(this.stack[this.base - 3], term, true);
    this.state = goto;
  }
};
var StackBufferCursor = class _StackBufferCursor {
  constructor(stack, pos, index) {
    this.stack = stack;
    this.pos = pos;
    this.index = index;
    this.buffer = stack.buffer;
    if (this.index == 0)
      this.maybeNext();
  }
  static create(stack, pos = stack.bufferBase + stack.buffer.length) {
    return new _StackBufferCursor(stack, pos, pos - stack.bufferBase);
  }
  maybeNext() {
    let next = this.stack.parent;
    if (next != null) {
      this.index = this.stack.bufferBase - next.bufferBase;
      this.stack = next;
      this.buffer = next.buffer;
    }
  }
  get id() {
    return this.buffer[this.index - 4];
  }
  get start() {
    return this.buffer[this.index - 3];
  }
  get end() {
    return this.buffer[this.index - 2];
  }
  get size() {
    return this.buffer[this.index - 1];
  }
  next() {
    this.index -= 4;
    this.pos -= 4;
    if (this.index == 0)
      this.maybeNext();
  }
  fork() {
    return new _StackBufferCursor(this.stack, this.pos, this.index);
  }
};
function decodeArray(input, Type = Uint16Array) {
  if (typeof input != "string")
    return input;
  let array = null;
  for (let pos = 0, out = 0; pos < input.length; ) {
    let value = 0;
    for (; ; ) {
      let next = input.charCodeAt(pos++), stop = false;
      if (next == 126) {
        value = 65535;
        break;
      }
      if (next >= 92)
        next--;
      if (next >= 34)
        next--;
      let digit = next - 32;
      if (digit >= 46) {
        digit -= 46;
        stop = true;
      }
      value += digit;
      if (stop)
        break;
      value *= 46;
    }
    if (array)
      array[out++] = value;
    else
      array = new Type(value);
  }
  return array;
}
var CachedToken = class {
  constructor() {
    this.start = -1;
    this.value = -1;
    this.end = -1;
    this.extended = -1;
    this.lookAhead = 0;
    this.mask = 0;
    this.context = 0;
  }
};
var nullToken = new CachedToken();
var InputStream = class {
  /**
  @internal
  */
  constructor(input, ranges) {
    this.input = input;
    this.ranges = ranges;
    this.chunk = "";
    this.chunkOff = 0;
    this.chunk2 = "";
    this.chunk2Pos = 0;
    this.next = -1;
    this.token = nullToken;
    this.rangeIndex = 0;
    this.pos = this.chunkPos = ranges[0].from;
    this.range = ranges[0];
    this.end = ranges[ranges.length - 1].to;
    this.readNext();
  }
  /**
  @internal
  */
  resolveOffset(offset, assoc) {
    let range = this.range, index = this.rangeIndex;
    let pos = this.pos + offset;
    while (pos < range.from) {
      if (!index)
        return null;
      let next = this.ranges[--index];
      pos -= range.from - next.to;
      range = next;
    }
    while (assoc < 0 ? pos > range.to : pos >= range.to) {
      if (index == this.ranges.length - 1)
        return null;
      let next = this.ranges[++index];
      pos += next.from - range.to;
      range = next;
    }
    return pos;
  }
  /**
  @internal
  */
  clipPos(pos) {
    if (pos >= this.range.from && pos < this.range.to)
      return pos;
    for (let range of this.ranges)
      if (range.to > pos)
        return Math.max(pos, range.from);
    return this.end;
  }
  /**
  Look at a code unit near the stream position. `.peek(0)` equals
  `.next`, `.peek(-1)` gives you the previous character, and so
  on.
  
  Note that looking around during tokenizing creates dependencies
  on potentially far-away content, which may reduce the
  effectiveness incremental parsing—when looking forward—or even
  cause invalid reparses when looking backward more than 25 code
  units, since the library does not track lookbehind.
  */
  peek(offset) {
    let idx = this.chunkOff + offset, pos, result;
    if (idx >= 0 && idx < this.chunk.length) {
      pos = this.pos + offset;
      result = this.chunk.charCodeAt(idx);
    } else {
      let resolved = this.resolveOffset(offset, 1);
      if (resolved == null)
        return -1;
      pos = resolved;
      if (pos >= this.chunk2Pos && pos < this.chunk2Pos + this.chunk2.length) {
        result = this.chunk2.charCodeAt(pos - this.chunk2Pos);
      } else {
        let i = this.rangeIndex, range = this.range;
        while (range.to <= pos)
          range = this.ranges[++i];
        this.chunk2 = this.input.chunk(this.chunk2Pos = pos);
        if (pos + this.chunk2.length > range.to)
          this.chunk2 = this.chunk2.slice(0, range.to - pos);
        result = this.chunk2.charCodeAt(0);
      }
    }
    if (pos >= this.token.lookAhead)
      this.token.lookAhead = pos + 1;
    return result;
  }
  /**
  Accept a token. By default, the end of the token is set to the
  current stream position, but you can pass an offset (relative to
  the stream position) to change that.
  */
  acceptToken(token, endOffset = 0) {
    let end = endOffset ? this.resolveOffset(endOffset, -1) : this.pos;
    if (end == null || end < this.token.start)
      throw new RangeError("Token end out of bounds");
    this.token.value = token;
    this.token.end = end;
  }
  /**
  Accept a token ending at a specific given position.
  */
  acceptTokenTo(token, endPos) {
    this.token.value = token;
    this.token.end = endPos;
  }
  getChunk() {
    if (this.pos >= this.chunk2Pos && this.pos < this.chunk2Pos + this.chunk2.length) {
      let { chunk, chunkPos } = this;
      this.chunk = this.chunk2;
      this.chunkPos = this.chunk2Pos;
      this.chunk2 = chunk;
      this.chunk2Pos = chunkPos;
      this.chunkOff = this.pos - this.chunkPos;
    } else {
      this.chunk2 = this.chunk;
      this.chunk2Pos = this.chunkPos;
      let nextChunk = this.input.chunk(this.pos);
      let end = this.pos + nextChunk.length;
      this.chunk = end > this.range.to ? nextChunk.slice(0, this.range.to - this.pos) : nextChunk;
      this.chunkPos = this.pos;
      this.chunkOff = 0;
    }
  }
  readNext() {
    if (this.chunkOff >= this.chunk.length) {
      this.getChunk();
      if (this.chunkOff == this.chunk.length)
        return this.next = -1;
    }
    return this.next = this.chunk.charCodeAt(this.chunkOff);
  }
  /**
  Move the stream forward N (defaults to 1) code units. Returns
  the new value of [`next`](#lr.InputStream.next).
  */
  advance(n = 1) {
    this.chunkOff += n;
    while (this.pos + n >= this.range.to) {
      if (this.rangeIndex == this.ranges.length - 1)
        return this.setDone();
      n -= this.range.to - this.pos;
      this.range = this.ranges[++this.rangeIndex];
      this.pos = this.range.from;
    }
    this.pos += n;
    if (this.pos >= this.token.lookAhead)
      this.token.lookAhead = this.pos + 1;
    return this.readNext();
  }
  setDone() {
    this.pos = this.chunkPos = this.end;
    this.range = this.ranges[this.rangeIndex = this.ranges.length - 1];
    this.chunk = "";
    return this.next = -1;
  }
  /**
  @internal
  */
  reset(pos, token) {
    if (token) {
      this.token = token;
      token.start = pos;
      token.lookAhead = pos + 1;
      token.value = token.extended = -1;
    } else {
      this.token = nullToken;
    }
    if (this.pos != pos) {
      this.pos = pos;
      if (pos == this.end) {
        this.setDone();
        return this;
      }
      while (pos < this.range.from)
        this.range = this.ranges[--this.rangeIndex];
      while (pos >= this.range.to)
        this.range = this.ranges[++this.rangeIndex];
      if (pos >= this.chunkPos && pos < this.chunkPos + this.chunk.length) {
        this.chunkOff = pos - this.chunkPos;
      } else {
        this.chunk = "";
        this.chunkOff = 0;
      }
      this.readNext();
    }
    return this;
  }
  /**
  @internal
  */
  read(from, to) {
    if (from >= this.chunkPos && to <= this.chunkPos + this.chunk.length)
      return this.chunk.slice(from - this.chunkPos, to - this.chunkPos);
    if (from >= this.chunk2Pos && to <= this.chunk2Pos + this.chunk2.length)
      return this.chunk2.slice(from - this.chunk2Pos, to - this.chunk2Pos);
    if (from >= this.range.from && to <= this.range.to)
      return this.input.read(from, to);
    let result = "";
    for (let r of this.ranges) {
      if (r.from >= to)
        break;
      if (r.to > from)
        result += this.input.read(Math.max(r.from, from), Math.min(r.to, to));
    }
    return result;
  }
};
var TokenGroup = class {
  constructor(data, id) {
    this.data = data;
    this.id = id;
  }
  token(input, stack) {
    let { parser: parser2 } = stack.p;
    readToken(this.data, input, stack, this.id, parser2.data, parser2.tokenPrecTable);
  }
};
TokenGroup.prototype.contextual = TokenGroup.prototype.fallback = TokenGroup.prototype.extend = false;
var LocalTokenGroup = class {
  constructor(data, precTable, elseToken) {
    this.precTable = precTable;
    this.elseToken = elseToken;
    this.data = typeof data == "string" ? decodeArray(data) : data;
  }
  token(input, stack) {
    let start = input.pos, skipped = 0;
    for (; ; ) {
      let atEof = input.next < 0, nextPos = input.resolveOffset(1, 1);
      readToken(this.data, input, stack, 0, this.data, this.precTable);
      if (input.token.value > -1)
        break;
      if (this.elseToken == null)
        return;
      if (!atEof)
        skipped++;
      if (nextPos == null)
        break;
      input.reset(nextPos, input.token);
    }
    if (skipped) {
      input.reset(start, input.token);
      input.acceptToken(this.elseToken, skipped);
    }
  }
};
LocalTokenGroup.prototype.contextual = TokenGroup.prototype.fallback = TokenGroup.prototype.extend = false;
var ExternalTokenizer = class {
  /**
  Create a tokenizer. The first argument is the function that,
  given an input stream, scans for the types of tokens it
  recognizes at the stream's position, and calls
  [`acceptToken`](#lr.InputStream.acceptToken) when it finds
  one.
  */
  constructor(token, options = {}) {
    this.token = token;
    this.contextual = !!options.contextual;
    this.fallback = !!options.fallback;
    this.extend = !!options.extend;
  }
};
function readToken(data, input, stack, group, precTable, precOffset) {
  let state = 0, groupMask = 1 << group, { dialect } = stack.p.parser;
  scan: for (; ; ) {
    if ((groupMask & data[state]) == 0)
      break;
    let accEnd = data[state + 1];
    for (let i = state + 3; i < accEnd; i += 2)
      if ((data[i + 1] & groupMask) > 0) {
        let term = data[i];
        if (dialect.allows(term) && (input.token.value == -1 || input.token.value == term || overrides(term, input.token.value, precTable, precOffset))) {
          input.acceptToken(term);
          break;
        }
      }
    let next = input.next, low = 0, high = data[state + 2];
    if (input.next < 0 && high > low && data[accEnd + high * 3 - 3] == 65535) {
      state = data[accEnd + high * 3 - 1];
      continue scan;
    }
    for (; low < high; ) {
      let mid = low + high >> 1;
      let index = accEnd + mid + (mid << 1);
      let from = data[index], to = data[index + 1] || 65536;
      if (next < from)
        high = mid;
      else if (next >= to)
        low = mid + 1;
      else {
        state = data[index + 2];
        input.advance();
        continue scan;
      }
    }
    break;
  }
}
function findOffset(data, start, term) {
  for (let i = start, next; (next = data[i]) != 65535; i++)
    if (next == term)
      return i - start;
  return -1;
}
function overrides(token, prev, tableData, tableOffset) {
  let iPrev = findOffset(tableData, tableOffset, prev);
  return iPrev < 0 || findOffset(tableData, tableOffset, token) < iPrev;
}
var verbose = typeof process != "undefined" && process.env && /\bparse\b/.test(process.env.LOG);
var stackIDs = null;
function cutAt(tree, pos, side) {
  let cursor = tree.cursor(IterMode.IncludeAnonymous);
  cursor.moveTo(pos);
  for (; ; ) {
    if (!(side < 0 ? cursor.childBefore(pos) : cursor.childAfter(pos)))
      for (; ; ) {
        if ((side < 0 ? cursor.to < pos : cursor.from > pos) && !cursor.type.isError)
          return side < 0 ? Math.max(0, Math.min(
            cursor.to - 1,
            pos - 25
            /* Lookahead.Margin */
          )) : Math.min(tree.length, Math.max(
            cursor.from + 1,
            pos + 25
            /* Lookahead.Margin */
          ));
        if (side < 0 ? cursor.prevSibling() : cursor.nextSibling())
          break;
        if (!cursor.parent())
          return side < 0 ? 0 : tree.length;
      }
  }
}
var FragmentCursor = class {
  constructor(fragments, nodeSet) {
    this.fragments = fragments;
    this.nodeSet = nodeSet;
    this.i = 0;
    this.fragment = null;
    this.safeFrom = -1;
    this.safeTo = -1;
    this.trees = [];
    this.start = [];
    this.index = [];
    this.nextFragment();
  }
  nextFragment() {
    let fr = this.fragment = this.i == this.fragments.length ? null : this.fragments[this.i++];
    if (fr) {
      this.safeFrom = fr.openStart ? cutAt(fr.tree, fr.from + fr.offset, 1) - fr.offset : fr.from;
      this.safeTo = fr.openEnd ? cutAt(fr.tree, fr.to + fr.offset, -1) - fr.offset : fr.to;
      while (this.trees.length) {
        this.trees.pop();
        this.start.pop();
        this.index.pop();
      }
      this.trees.push(fr.tree);
      this.start.push(-fr.offset);
      this.index.push(0);
      this.nextStart = this.safeFrom;
    } else {
      this.nextStart = 1e9;
    }
  }
  // `pos` must be >= any previously given `pos` for this cursor
  nodeAt(pos) {
    if (pos < this.nextStart)
      return null;
    while (this.fragment && this.safeTo <= pos)
      this.nextFragment();
    if (!this.fragment)
      return null;
    for (; ; ) {
      let last = this.trees.length - 1;
      if (last < 0) {
        this.nextFragment();
        return null;
      }
      let top2 = this.trees[last], index = this.index[last];
      if (index == top2.children.length) {
        this.trees.pop();
        this.start.pop();
        this.index.pop();
        continue;
      }
      let next = top2.children[index];
      let start = this.start[last] + top2.positions[index];
      if (start > pos) {
        this.nextStart = start;
        return null;
      }
      if (next instanceof Tree) {
        if (start == pos) {
          if (start < this.safeFrom)
            return null;
          let end = start + next.length;
          if (end <= this.safeTo) {
            let lookAhead = next.prop(NodeProp.lookAhead);
            if (!lookAhead || end + lookAhead < this.fragment.to)
              return next;
          }
        }
        this.index[last]++;
        if (start + next.length >= Math.max(this.safeFrom, pos)) {
          this.trees.push(next);
          this.start.push(start);
          this.index.push(0);
        }
      } else {
        this.index[last]++;
        this.nextStart = start + next.length;
      }
    }
  }
};
var TokenCache = class {
  constructor(parser2, stream) {
    this.stream = stream;
    this.tokens = [];
    this.mainToken = null;
    this.actions = [];
    this.tokens = parser2.tokenizers.map((_) => new CachedToken());
  }
  getActions(stack) {
    let actionIndex = 0;
    let main = null;
    let { parser: parser2 } = stack.p, { tokenizers } = parser2;
    let mask = parser2.stateSlot(
      stack.state,
      3
      /* ParseState.TokenizerMask */
    );
    let context = stack.curContext ? stack.curContext.hash : 0;
    let lookAhead = 0;
    for (let i = 0; i < tokenizers.length; i++) {
      if ((1 << i & mask) == 0)
        continue;
      let tokenizer = tokenizers[i], token = this.tokens[i];
      if (main && !tokenizer.fallback)
        continue;
      if (tokenizer.contextual || token.start != stack.pos || token.mask != mask || token.context != context) {
        this.updateCachedToken(token, tokenizer, stack);
        token.mask = mask;
        token.context = context;
      }
      if (token.lookAhead > token.end + 25)
        lookAhead = Math.max(token.lookAhead, lookAhead);
      if (token.value != 0) {
        let startIndex = actionIndex;
        if (token.extended > -1)
          actionIndex = this.addActions(stack, token.extended, token.end, actionIndex);
        actionIndex = this.addActions(stack, token.value, token.end, actionIndex);
        if (!tokenizer.extend) {
          main = token;
          if (actionIndex > startIndex)
            break;
        }
      }
    }
    while (this.actions.length > actionIndex)
      this.actions.pop();
    if (lookAhead)
      stack.setLookAhead(lookAhead);
    if (!main && stack.pos == this.stream.end) {
      main = new CachedToken();
      main.value = stack.p.parser.eofTerm;
      main.start = main.end = stack.pos;
      actionIndex = this.addActions(stack, main.value, main.end, actionIndex);
    }
    this.mainToken = main;
    return this.actions;
  }
  getMainToken(stack) {
    if (this.mainToken)
      return this.mainToken;
    let main = new CachedToken(), { pos, p } = stack;
    main.start = pos;
    main.end = Math.min(pos + 1, p.stream.end);
    main.value = pos == p.stream.end ? p.parser.eofTerm : 0;
    return main;
  }
  updateCachedToken(token, tokenizer, stack) {
    let start = this.stream.clipPos(stack.pos);
    tokenizer.token(this.stream.reset(start, token), stack);
    if (token.value > -1) {
      let { parser: parser2 } = stack.p;
      for (let i = 0; i < parser2.specialized.length; i++)
        if (parser2.specialized[i] == token.value) {
          let result = parser2.specializers[i](this.stream.read(token.start, token.end), stack);
          if (result >= 0 && stack.p.parser.dialect.allows(result >> 1)) {
            if ((result & 1) == 0)
              token.value = result >> 1;
            else
              token.extended = result >> 1;
            break;
          }
        }
    } else {
      token.value = 0;
      token.end = this.stream.clipPos(start + 1);
    }
  }
  putAction(action, token, end, index) {
    for (let i = 0; i < index; i += 3)
      if (this.actions[i] == action)
        return index;
    this.actions[index++] = action;
    this.actions[index++] = token;
    this.actions[index++] = end;
    return index;
  }
  addActions(stack, token, end, index) {
    let { state } = stack, { parser: parser2 } = stack.p, { data } = parser2;
    for (let set = 0; set < 2; set++) {
      for (let i = parser2.stateSlot(
        state,
        set ? 2 : 1
        /* ParseState.Actions */
      ); ; i += 3) {
        if (data[i] == 65535) {
          if (data[i + 1] == 1) {
            i = pair(data, i + 2);
          } else {
            if (index == 0 && data[i + 1] == 2)
              index = this.putAction(pair(data, i + 2), token, end, index);
            break;
          }
        }
        if (data[i] == token)
          index = this.putAction(pair(data, i + 1), token, end, index);
      }
    }
    return index;
  }
};
var Parse = class {
  constructor(parser2, input, fragments, ranges) {
    this.parser = parser2;
    this.input = input;
    this.ranges = ranges;
    this.recovering = 0;
    this.nextStackID = 9812;
    this.minStackPos = 0;
    this.reused = [];
    this.stoppedAt = null;
    this.lastBigReductionStart = -1;
    this.lastBigReductionSize = 0;
    this.bigReductionCount = 0;
    this.stream = new InputStream(input, ranges);
    this.tokens = new TokenCache(parser2, this.stream);
    this.topTerm = parser2.top[1];
    let { from } = ranges[0];
    this.stacks = [Stack.start(this, parser2.top[0], from)];
    this.fragments = fragments.length && this.stream.end - from > parser2.bufferLength * 4 ? new FragmentCursor(fragments, parser2.nodeSet) : null;
  }
  get parsedPos() {
    return this.minStackPos;
  }
  // Move the parser forward. This will process all parse stacks at
  // `this.pos` and try to advance them to a further position. If no
  // stack for such a position is found, it'll start error-recovery.
  //
  // When the parse is finished, this will return a syntax tree. When
  // not, it returns `null`.
  advance() {
    let stacks = this.stacks, pos = this.minStackPos;
    let newStacks = this.stacks = [];
    let stopped, stoppedTokens;
    if (this.bigReductionCount > 300 && stacks.length == 1) {
      let [s] = stacks;
      while (s.forceReduce() && s.stack.length && s.stack[s.stack.length - 2] >= this.lastBigReductionStart) {
      }
      this.bigReductionCount = this.lastBigReductionSize = 0;
    }
    for (let i = 0; i < stacks.length; i++) {
      let stack = stacks[i];
      for (; ; ) {
        this.tokens.mainToken = null;
        if (stack.pos > pos) {
          newStacks.push(stack);
        } else if (this.advanceStack(stack, newStacks, stacks)) {
          continue;
        } else {
          if (!stopped) {
            stopped = [];
            stoppedTokens = [];
          }
          stopped.push(stack);
          let tok = this.tokens.getMainToken(stack);
          stoppedTokens.push(tok.value, tok.end);
        }
        break;
      }
    }
    if (!newStacks.length) {
      let finished = stopped && findFinished(stopped);
      if (finished) {
        if (verbose)
          console.log("Finish with " + this.stackID(finished));
        return this.stackToTree(finished);
      }
      if (this.parser.strict) {
        if (verbose && stopped)
          console.log("Stuck with token " + (this.tokens.mainToken ? this.parser.getName(this.tokens.mainToken.value) : "none"));
        throw new SyntaxError("No parse at " + pos);
      }
      if (!this.recovering)
        this.recovering = 5;
    }
    if (this.recovering && stopped) {
      let finished = this.stoppedAt != null && stopped[0].pos > this.stoppedAt ? stopped[0] : this.runRecovery(stopped, stoppedTokens, newStacks);
      if (finished) {
        if (verbose)
          console.log("Force-finish " + this.stackID(finished));
        return this.stackToTree(finished.forceAll());
      }
    }
    if (this.recovering) {
      let maxRemaining = this.recovering == 1 ? 1 : this.recovering * 3;
      if (newStacks.length > maxRemaining) {
        newStacks.sort((a2, b) => b.score - a2.score);
        while (newStacks.length > maxRemaining)
          newStacks.pop();
      }
      if (newStacks.some((s) => s.reducePos > pos))
        this.recovering--;
    } else if (newStacks.length > 1) {
      outer: for (let i = 0; i < newStacks.length - 1; i++) {
        let stack = newStacks[i];
        for (let j = i + 1; j < newStacks.length; j++) {
          let other = newStacks[j];
          if (stack.sameState(other) || stack.buffer.length > 500 && other.buffer.length > 500) {
            if ((stack.score - other.score || stack.buffer.length - other.buffer.length) > 0) {
              newStacks.splice(j--, 1);
            } else {
              newStacks.splice(i--, 1);
              continue outer;
            }
          }
        }
      }
      if (newStacks.length > 12)
        newStacks.splice(
          12,
          newStacks.length - 12
          /* Rec.MaxStackCount */
        );
    }
    this.minStackPos = newStacks[0].pos;
    for (let i = 1; i < newStacks.length; i++)
      if (newStacks[i].pos < this.minStackPos)
        this.minStackPos = newStacks[i].pos;
    return null;
  }
  stopAt(pos) {
    if (this.stoppedAt != null && this.stoppedAt < pos)
      throw new RangeError("Can't move stoppedAt forward");
    this.stoppedAt = pos;
  }
  // Returns an updated version of the given stack, or null if the
  // stack can't advance normally. When `split` and `stacks` are
  // given, stacks split off by ambiguous operations will be pushed to
  // `split`, or added to `stacks` if they move `pos` forward.
  advanceStack(stack, stacks, split) {
    let start = stack.pos, { parser: parser2 } = this;
    let base2 = verbose ? this.stackID(stack) + " -> " : "";
    if (this.stoppedAt != null && start > this.stoppedAt)
      return stack.forceReduce() ? stack : null;
    if (this.fragments) {
      let strictCx = stack.curContext && stack.curContext.tracker.strict, cxHash = strictCx ? stack.curContext.hash : 0;
      for (let cached = this.fragments.nodeAt(start); cached; ) {
        let match = this.parser.nodeSet.types[cached.type.id] == cached.type ? parser2.getGoto(stack.state, cached.type.id) : -1;
        if (match > -1 && cached.length && (!strictCx || (cached.prop(NodeProp.contextHash) || 0) == cxHash)) {
          stack.useNode(cached, match);
          if (verbose)
            console.log(base2 + this.stackID(stack) + ` (via reuse of ${parser2.getName(cached.type.id)})`);
          return true;
        }
        if (!(cached instanceof Tree) || cached.children.length == 0 || cached.positions[0] > 0)
          break;
        let inner = cached.children[0];
        if (inner instanceof Tree && cached.positions[0] == 0)
          cached = inner;
        else
          break;
      }
    }
    let defaultReduce = parser2.stateSlot(
      stack.state,
      4
      /* ParseState.DefaultReduce */
    );
    if (defaultReduce > 0) {
      stack.reduce(defaultReduce);
      if (verbose)
        console.log(base2 + this.stackID(stack) + ` (via always-reduce ${parser2.getName(
          defaultReduce & 65535
          /* Action.ValueMask */
        )})`);
      return true;
    }
    if (stack.stack.length >= 8400) {
      while (stack.stack.length > 6e3 && stack.forceReduce()) {
      }
    }
    let actions = this.tokens.getActions(stack);
    for (let i = 0; i < actions.length; ) {
      let action = actions[i++], term = actions[i++], end = actions[i++];
      let last = i == actions.length || !split;
      let localStack = last ? stack : stack.split();
      let main = this.tokens.mainToken;
      localStack.apply(action, term, main ? main.start : localStack.pos, end);
      if (verbose)
        console.log(base2 + this.stackID(localStack) + ` (via ${(action & 65536) == 0 ? "shift" : `reduce of ${parser2.getName(
          action & 65535
          /* Action.ValueMask */
        )}`} for ${parser2.getName(term)} @ ${start}${localStack == stack ? "" : ", split"})`);
      if (last)
        return true;
      else if (localStack.pos > start)
        stacks.push(localStack);
      else
        split.push(localStack);
    }
    return false;
  }
  // Advance a given stack forward as far as it will go. Returns the
  // (possibly updated) stack if it got stuck, or null if it moved
  // forward and was given to `pushStackDedup`.
  advanceFully(stack, newStacks) {
    let pos = stack.pos;
    for (; ; ) {
      if (!this.advanceStack(stack, null, null))
        return false;
      if (stack.pos > pos) {
        pushStackDedup(stack, newStacks);
        return true;
      }
    }
  }
  runRecovery(stacks, tokens, newStacks) {
    let finished = null, restarted = false;
    for (let i = 0; i < stacks.length; i++) {
      let stack = stacks[i], token = tokens[i << 1], tokenEnd = tokens[(i << 1) + 1];
      let base2 = verbose ? this.stackID(stack) + " -> " : "";
      if (stack.deadEnd) {
        if (restarted)
          continue;
        restarted = true;
        stack.restart();
        if (verbose)
          console.log(base2 + this.stackID(stack) + " (restarted)");
        let done = this.advanceFully(stack, newStacks);
        if (done)
          continue;
      }
      let force = stack.split(), forceBase = base2;
      for (let j = 0; j < 10 && force.forceReduce(); j++) {
        if (verbose)
          console.log(forceBase + this.stackID(force) + " (via force-reduce)");
        let done = this.advanceFully(force, newStacks);
        if (done)
          break;
        if (verbose)
          forceBase = this.stackID(force) + " -> ";
      }
      for (let insert2 of stack.recoverByInsert(token)) {
        if (verbose)
          console.log(base2 + this.stackID(insert2) + " (via recover-insert)");
        this.advanceFully(insert2, newStacks);
      }
      if (this.stream.end > stack.pos) {
        if (tokenEnd == stack.pos) {
          tokenEnd++;
          token = 0;
        }
        stack.recoverByDelete(token, tokenEnd);
        if (verbose)
          console.log(base2 + this.stackID(stack) + ` (via recover-delete ${this.parser.getName(token)})`);
        pushStackDedup(stack, newStacks);
      } else if (!finished || finished.score < stack.score) {
        finished = stack;
      }
    }
    return finished;
  }
  // Convert the stack's buffer to a syntax tree.
  stackToTree(stack) {
    stack.close();
    return Tree.build({
      buffer: StackBufferCursor.create(stack),
      nodeSet: this.parser.nodeSet,
      topID: this.topTerm,
      maxBufferLength: this.parser.bufferLength,
      reused: this.reused,
      start: this.ranges[0].from,
      length: stack.pos - this.ranges[0].from,
      minRepeatType: this.parser.minRepeatTerm
    });
  }
  stackID(stack) {
    let id = (stackIDs || (stackIDs = /* @__PURE__ */ new WeakMap())).get(stack);
    if (!id)
      stackIDs.set(stack, id = String.fromCodePoint(this.nextStackID++));
    return id + stack;
  }
};
function pushStackDedup(stack, newStacks) {
  for (let i = 0; i < newStacks.length; i++) {
    let other = newStacks[i];
    if (other.pos == stack.pos && other.sameState(stack)) {
      if (newStacks[i].score < stack.score)
        newStacks[i] = stack;
      return;
    }
  }
  newStacks.push(stack);
}
var Dialect = class {
  constructor(source, flags, disabled) {
    this.source = source;
    this.flags = flags;
    this.disabled = disabled;
  }
  allows(term) {
    return !this.disabled || this.disabled[term] == 0;
  }
};
var LRParser = class _LRParser extends Parser {
  /**
  @internal
  */
  constructor(spec) {
    super();
    this.wrappers = [];
    if (spec.version != 14)
      throw new RangeError(`Parser version (${spec.version}) doesn't match runtime version (${14})`);
    let nodeNames = spec.nodeNames.split(" ");
    this.minRepeatTerm = nodeNames.length;
    for (let i = 0; i < spec.repeatNodeCount; i++)
      nodeNames.push("");
    let topTerms = Object.keys(spec.topRules).map((r) => spec.topRules[r][1]);
    let nodeProps = [];
    for (let i = 0; i < nodeNames.length; i++)
      nodeProps.push([]);
    function setProp(nodeID, prop, value) {
      nodeProps[nodeID].push([prop, prop.deserialize(String(value))]);
    }
    if (spec.nodeProps)
      for (let propSpec of spec.nodeProps) {
        let prop = propSpec[0];
        if (typeof prop == "string")
          prop = NodeProp[prop];
        for (let i = 1; i < propSpec.length; ) {
          let next = propSpec[i++];
          if (next >= 0) {
            setProp(next, prop, propSpec[i++]);
          } else {
            let value = propSpec[i + -next];
            for (let j = -next; j > 0; j--)
              setProp(propSpec[i++], prop, value);
            i++;
          }
        }
      }
    this.nodeSet = new NodeSet(nodeNames.map((name2, i) => NodeType.define({
      name: i >= this.minRepeatTerm ? void 0 : name2,
      id: i,
      props: nodeProps[i],
      top: topTerms.indexOf(i) > -1,
      error: i == 0,
      skipped: spec.skippedNodes && spec.skippedNodes.indexOf(i) > -1
    })));
    if (spec.propSources)
      this.nodeSet = this.nodeSet.extend(...spec.propSources);
    this.strict = false;
    this.bufferLength = DefaultBufferLength;
    let tokenArray = decodeArray(spec.tokenData);
    this.context = spec.context;
    this.specializerSpecs = spec.specialized || [];
    this.specialized = new Uint16Array(this.specializerSpecs.length);
    for (let i = 0; i < this.specializerSpecs.length; i++)
      this.specialized[i] = this.specializerSpecs[i].term;
    this.specializers = this.specializerSpecs.map(getSpecializer);
    this.states = decodeArray(spec.states, Uint32Array);
    this.data = decodeArray(spec.stateData);
    this.goto = decodeArray(spec.goto);
    this.maxTerm = spec.maxTerm;
    this.tokenizers = spec.tokenizers.map((value) => typeof value == "number" ? new TokenGroup(tokenArray, value) : value);
    this.topRules = spec.topRules;
    this.dialects = spec.dialects || {};
    this.dynamicPrecedences = spec.dynamicPrecedences || null;
    this.tokenPrecTable = spec.tokenPrec;
    this.termNames = spec.termNames || null;
    this.maxNode = this.nodeSet.types.length - 1;
    this.dialect = this.parseDialect();
    this.top = this.topRules[Object.keys(this.topRules)[0]];
  }
  createParse(input, fragments, ranges) {
    let parse = new Parse(this, input, fragments, ranges);
    for (let w of this.wrappers)
      parse = w(parse, input, fragments, ranges);
    return parse;
  }
  /**
  Get a goto table entry @internal
  */
  getGoto(state, term, loose = false) {
    let table = this.goto;
    if (term >= table[0])
      return -1;
    for (let pos = table[term + 1]; ; ) {
      let groupTag = table[pos++], last = groupTag & 1;
      let target = table[pos++];
      if (last && loose)
        return target;
      for (let end = pos + (groupTag >> 1); pos < end; pos++)
        if (table[pos] == state)
          return target;
      if (last)
        return -1;
    }
  }
  /**
  Check if this state has an action for a given terminal @internal
  */
  hasAction(state, terminal) {
    let data = this.data;
    for (let set = 0; set < 2; set++) {
      for (let i = this.stateSlot(
        state,
        set ? 2 : 1
        /* ParseState.Actions */
      ), next; ; i += 3) {
        if ((next = data[i]) == 65535) {
          if (data[i + 1] == 1)
            next = data[i = pair(data, i + 2)];
          else if (data[i + 1] == 2)
            return pair(data, i + 2);
          else
            break;
        }
        if (next == terminal || next == 0)
          return pair(data, i + 1);
      }
    }
    return 0;
  }
  /**
  @internal
  */
  stateSlot(state, slot) {
    return this.states[state * 6 + slot];
  }
  /**
  @internal
  */
  stateFlag(state, flag) {
    return (this.stateSlot(
      state,
      0
      /* ParseState.Flags */
    ) & flag) > 0;
  }
  /**
  @internal
  */
  validAction(state, action) {
    return !!this.allActions(state, (a2) => a2 == action ? true : null);
  }
  /**
  @internal
  */
  allActions(state, action) {
    let deflt = this.stateSlot(
      state,
      4
      /* ParseState.DefaultReduce */
    );
    let result = deflt ? action(deflt) : void 0;
    for (let i = this.stateSlot(
      state,
      1
      /* ParseState.Actions */
    ); result == null; i += 3) {
      if (this.data[i] == 65535) {
        if (this.data[i + 1] == 1)
          i = pair(this.data, i + 2);
        else
          break;
      }
      result = action(pair(this.data, i + 1));
    }
    return result;
  }
  /**
  Get the states that can follow this one through shift actions or
  goto jumps. @internal
  */
  nextStates(state) {
    let result = [];
    for (let i = this.stateSlot(
      state,
      1
      /* ParseState.Actions */
    ); ; i += 3) {
      if (this.data[i] == 65535) {
        if (this.data[i + 1] == 1)
          i = pair(this.data, i + 2);
        else
          break;
      }
      if ((this.data[i + 2] & 65536 >> 16) == 0) {
        let value = this.data[i + 1];
        if (!result.some((v, i2) => i2 & 1 && v == value))
          result.push(this.data[i], value);
      }
    }
    return result;
  }
  /**
  Configure the parser. Returns a new parser instance that has the
  given settings modified. Settings not provided in `config` are
  kept from the original parser.
  */
  configure(config) {
    let copy = Object.assign(Object.create(_LRParser.prototype), this);
    if (config.props)
      copy.nodeSet = this.nodeSet.extend(...config.props);
    if (config.top) {
      let info = this.topRules[config.top];
      if (!info)
        throw new RangeError(`Invalid top rule name ${config.top}`);
      copy.top = info;
    }
    if (config.tokenizers)
      copy.tokenizers = this.tokenizers.map((t2) => {
        let found = config.tokenizers.find((r) => r.from == t2);
        return found ? found.to : t2;
      });
    if (config.specializers) {
      copy.specializers = this.specializers.slice();
      copy.specializerSpecs = this.specializerSpecs.map((s, i) => {
        let found = config.specializers.find((r) => r.from == s.external);
        if (!found)
          return s;
        let spec = Object.assign(Object.assign({}, s), { external: found.to });
        copy.specializers[i] = getSpecializer(spec);
        return spec;
      });
    }
    if (config.contextTracker)
      copy.context = config.contextTracker;
    if (config.dialect)
      copy.dialect = this.parseDialect(config.dialect);
    if (config.strict != null)
      copy.strict = config.strict;
    if (config.wrap)
      copy.wrappers = copy.wrappers.concat(config.wrap);
    if (config.bufferLength != null)
      copy.bufferLength = config.bufferLength;
    return copy;
  }
  /**
  Tells you whether any [parse wrappers](#lr.ParserConfig.wrap)
  are registered for this parser.
  */
  hasWrappers() {
    return this.wrappers.length > 0;
  }
  /**
  Returns the name associated with a given term. This will only
  work for all terms when the parser was generated with the
  `--names` option. By default, only the names of tagged terms are
  stored.
  */
  getName(term) {
    return this.termNames ? this.termNames[term] : String(term <= this.maxNode && this.nodeSet.types[term].name || term);
  }
  /**
  The eof term id is always allocated directly after the node
  types. @internal
  */
  get eofTerm() {
    return this.maxNode + 1;
  }
  /**
  The type of top node produced by the parser.
  */
  get topNode() {
    return this.nodeSet.types[this.top[1]];
  }
  /**
  @internal
  */
  dynamicPrecedence(term) {
    let prec2 = this.dynamicPrecedences;
    return prec2 == null ? 0 : prec2[term] || 0;
  }
  /**
  @internal
  */
  parseDialect(dialect) {
    let values = Object.keys(this.dialects), flags = values.map(() => false);
    if (dialect)
      for (let part of dialect.split(" ")) {
        let id = values.indexOf(part);
        if (id >= 0)
          flags[id] = true;
      }
    let disabled = null;
    for (let i = 0; i < values.length; i++)
      if (!flags[i]) {
        for (let j = this.dialects[values[i]], id; (id = this.data[j++]) != 65535; )
          (disabled || (disabled = new Uint8Array(this.maxTerm + 1)))[id] = 1;
      }
    return new Dialect(dialect, flags, disabled);
  }
  /**
  Used by the output of the parser generator. Not available to
  user code. @hide
  */
  static deserialize(spec) {
    return new _LRParser(spec);
  }
};
function pair(data, off) {
  return data[off] | data[off + 1] << 16;
}
function findFinished(stacks) {
  let best = null;
  for (let stack of stacks) {
    let stopped = stack.p.stoppedAt;
    if ((stack.pos == stack.p.stream.end || stopped != null && stack.pos > stopped) && stack.p.parser.stateFlag(
      stack.state,
      2
      /* StateFlag.Accepting */
    ) && (!best || best.score < stack.score))
      best = stack;
  }
  return best;
}
function getSpecializer(spec) {
  if (spec.external) {
    let mask = spec.extend ? 1 : 0;
    return (value, stack) => spec.external(value, stack) << 1 | mask;
  }
  return spec.get;
}

// node_modules/@lezer/highlight/dist/index.js
var nextTagID = 0;
var Tag = class _Tag {
  /**
  @internal
  */
  constructor(name2, set, base2, modified) {
    this.name = name2;
    this.set = set;
    this.base = base2;
    this.modified = modified;
    this.id = nextTagID++;
  }
  toString() {
    let { name: name2 } = this;
    for (let mod of this.modified)
      if (mod.name)
        name2 = `${mod.name}(${name2})`;
    return name2;
  }
  static define(nameOrParent, parent) {
    let name2 = typeof nameOrParent == "string" ? nameOrParent : "?";
    if (nameOrParent instanceof _Tag)
      parent = nameOrParent;
    if (parent === null || parent === void 0 ? void 0 : parent.base)
      throw new Error("Can not derive from a modified tag");
    let tag = new _Tag(name2, [], null, []);
    tag.set.push(tag);
    if (parent)
      for (let t2 of parent.set)
        tag.set.push(t2);
    return tag;
  }
  /**
  Define a tag _modifier_, which is a function that, given a tag,
  will return a tag that is a subtag of the original. Applying the
  same modifier to a twice tag will return the same value (`m1(t1)
  == m1(t1)`) and applying multiple modifiers will, regardless or
  order, produce the same tag (`m1(m2(t1)) == m2(m1(t1))`).
  
  When multiple modifiers are applied to a given base tag, each
  smaller set of modifiers is registered as a parent, so that for
  example `m1(m2(m3(t1)))` is a subtype of `m1(m2(t1))`,
  `m1(m3(t1)`, and so on.
  */
  static defineModifier(name2) {
    let mod = new Modifier(name2);
    return (tag) => {
      if (tag.modified.indexOf(mod) > -1)
        return tag;
      return Modifier.get(tag.base || tag, tag.modified.concat(mod).sort((a2, b) => a2.id - b.id));
    };
  }
};
var nextModifierID = 0;
var Modifier = class _Modifier {
  constructor(name2) {
    this.name = name2;
    this.instances = [];
    this.id = nextModifierID++;
  }
  static get(base2, mods) {
    if (!mods.length)
      return base2;
    let exists = mods[0].instances.find((t2) => t2.base == base2 && sameArray(mods, t2.modified));
    if (exists)
      return exists;
    let set = [], tag = new Tag(base2.name, set, base2, mods);
    for (let m of mods)
      m.instances.push(tag);
    let configs = powerSet(mods);
    for (let parent of base2.set)
      if (!parent.modified.length)
        for (let config of configs)
          set.push(_Modifier.get(parent, config));
    return tag;
  }
};
function sameArray(a2, b) {
  return a2.length == b.length && a2.every((x, i) => x == b[i]);
}
function powerSet(array) {
  let sets = [[]];
  for (let i = 0; i < array.length; i++) {
    for (let j = 0, e = sets.length; j < e; j++) {
      sets.push(sets[j].concat(array[i]));
    }
  }
  return sets.sort((a2, b) => b.length - a2.length);
}
function styleTags(spec) {
  let byName = /* @__PURE__ */ Object.create(null);
  for (let prop in spec) {
    let tags2 = spec[prop];
    if (!Array.isArray(tags2))
      tags2 = [tags2];
    for (let part of prop.split(" "))
      if (part) {
        let pieces = [], mode = 2, rest = part;
        for (let pos = 0; ; ) {
          if (rest == "..." && pos > 0 && pos + 3 == part.length) {
            mode = 1;
            break;
          }
          let m = /^"(?:[^"\\]|\\.)*?"|[^\/!]+/.exec(rest);
          if (!m)
            throw new RangeError("Invalid path: " + part);
          pieces.push(m[0] == "*" ? "" : m[0][0] == '"' ? JSON.parse(m[0]) : m[0]);
          pos += m[0].length;
          if (pos == part.length)
            break;
          let next = part[pos++];
          if (pos == part.length && next == "!") {
            mode = 0;
            break;
          }
          if (next != "/")
            throw new RangeError("Invalid path: " + part);
          rest = part.slice(pos);
        }
        let last = pieces.length - 1, inner = pieces[last];
        if (!inner)
          throw new RangeError("Invalid path: " + part);
        let rule = new Rule(tags2, mode, last > 0 ? pieces.slice(0, last) : null);
        byName[inner] = rule.sort(byName[inner]);
      }
  }
  return ruleNodeProp.add(byName);
}
var ruleNodeProp = new NodeProp({
  combine(a2, b) {
    let cur, root, take;
    while (a2 || b) {
      if (!a2 || b && a2.depth >= b.depth) {
        take = b;
        b = b.next;
      } else {
        take = a2;
        a2 = a2.next;
      }
      if (cur && cur.mode == take.mode && !take.context && !cur.context)
        continue;
      let copy = new Rule(take.tags, take.mode, take.context);
      if (cur)
        cur.next = copy;
      else
        root = copy;
      cur = copy;
    }
    return root;
  }
});
var Rule = class {
  constructor(tags2, mode, context, next) {
    this.tags = tags2;
    this.mode = mode;
    this.context = context;
    this.next = next;
  }
  get opaque() {
    return this.mode == 0;
  }
  get inherit() {
    return this.mode == 1;
  }
  sort(other) {
    if (!other || other.depth < this.depth) {
      this.next = other;
      return this;
    }
    other.next = this.sort(other.next);
    return other;
  }
  get depth() {
    return this.context ? this.context.length : 0;
  }
};
Rule.empty = new Rule([], 2, null);
function tagHighlighter(tags2, options) {
  let map = /* @__PURE__ */ Object.create(null);
  for (let style of tags2) {
    if (!Array.isArray(style.tag))
      map[style.tag.id] = style.class;
    else
      for (let tag of style.tag)
        map[tag.id] = style.class;
  }
  let { scope, all = null } = options || {};
  return {
    style: (tags3) => {
      let cls = all;
      for (let tag of tags3) {
        for (let sub of tag.set) {
          let tagClass = map[sub.id];
          if (tagClass) {
            cls = cls ? cls + " " + tagClass : tagClass;
            break;
          }
        }
      }
      return cls;
    },
    scope
  };
}
var t = Tag.define;
var comment = t();
var name = t();
var typeName = t(name);
var propertyName = t(name);
var literal = t();
var string = t(literal);
var number = t(literal);
var content = t();
var heading = t(content);
var keyword = t();
var operator = t();
var punctuation = t();
var bracket = t(punctuation);
var meta = t();
var tags = {
  /**
  A comment.
  */
  comment,
  /**
  A line [comment](#highlight.tags.comment).
  */
  lineComment: t(comment),
  /**
  A block [comment](#highlight.tags.comment).
  */
  blockComment: t(comment),
  /**
  A documentation [comment](#highlight.tags.comment).
  */
  docComment: t(comment),
  /**
  Any kind of identifier.
  */
  name,
  /**
  The [name](#highlight.tags.name) of a variable.
  */
  variableName: t(name),
  /**
  A type [name](#highlight.tags.name).
  */
  typeName,
  /**
  A tag name (subtag of [`typeName`](#highlight.tags.typeName)).
  */
  tagName: t(typeName),
  /**
  A property or field [name](#highlight.tags.name).
  */
  propertyName,
  /**
  An attribute name (subtag of [`propertyName`](#highlight.tags.propertyName)).
  */
  attributeName: t(propertyName),
  /**
  The [name](#highlight.tags.name) of a class.
  */
  className: t(name),
  /**
  A label [name](#highlight.tags.name).
  */
  labelName: t(name),
  /**
  A namespace [name](#highlight.tags.name).
  */
  namespace: t(name),
  /**
  The [name](#highlight.tags.name) of a macro.
  */
  macroName: t(name),
  /**
  A literal value.
  */
  literal,
  /**
  A string [literal](#highlight.tags.literal).
  */
  string,
  /**
  A documentation [string](#highlight.tags.string).
  */
  docString: t(string),
  /**
  A character literal (subtag of [string](#highlight.tags.string)).
  */
  character: t(string),
  /**
  An attribute value (subtag of [string](#highlight.tags.string)).
  */
  attributeValue: t(string),
  /**
  A number [literal](#highlight.tags.literal).
  */
  number,
  /**
  An integer [number](#highlight.tags.number) literal.
  */
  integer: t(number),
  /**
  A floating-point [number](#highlight.tags.number) literal.
  */
  float: t(number),
  /**
  A boolean [literal](#highlight.tags.literal).
  */
  bool: t(literal),
  /**
  Regular expression [literal](#highlight.tags.literal).
  */
  regexp: t(literal),
  /**
  An escape [literal](#highlight.tags.literal), for example a
  backslash escape in a string.
  */
  escape: t(literal),
  /**
  A color [literal](#highlight.tags.literal).
  */
  color: t(literal),
  /**
  A URL [literal](#highlight.tags.literal).
  */
  url: t(literal),
  /**
  A language keyword.
  */
  keyword,
  /**
  The [keyword](#highlight.tags.keyword) for the self or this
  object.
  */
  self: t(keyword),
  /**
  The [keyword](#highlight.tags.keyword) for null.
  */
  null: t(keyword),
  /**
  A [keyword](#highlight.tags.keyword) denoting some atomic value.
  */
  atom: t(keyword),
  /**
  A [keyword](#highlight.tags.keyword) that represents a unit.
  */
  unit: t(keyword),
  /**
  A modifier [keyword](#highlight.tags.keyword).
  */
  modifier: t(keyword),
  /**
  A [keyword](#highlight.tags.keyword) that acts as an operator.
  */
  operatorKeyword: t(keyword),
  /**
  A control-flow related [keyword](#highlight.tags.keyword).
  */
  controlKeyword: t(keyword),
  /**
  A [keyword](#highlight.tags.keyword) that defines something.
  */
  definitionKeyword: t(keyword),
  /**
  A [keyword](#highlight.tags.keyword) related to defining or
  interfacing with modules.
  */
  moduleKeyword: t(keyword),
  /**
  An operator.
  */
  operator,
  /**
  An [operator](#highlight.tags.operator) that dereferences something.
  */
  derefOperator: t(operator),
  /**
  Arithmetic-related [operator](#highlight.tags.operator).
  */
  arithmeticOperator: t(operator),
  /**
  Logical [operator](#highlight.tags.operator).
  */
  logicOperator: t(operator),
  /**
  Bit [operator](#highlight.tags.operator).
  */
  bitwiseOperator: t(operator),
  /**
  Comparison [operator](#highlight.tags.operator).
  */
  compareOperator: t(operator),
  /**
  [Operator](#highlight.tags.operator) that updates its operand.
  */
  updateOperator: t(operator),
  /**
  [Operator](#highlight.tags.operator) that defines something.
  */
  definitionOperator: t(operator),
  /**
  Type-related [operator](#highlight.tags.operator).
  */
  typeOperator: t(operator),
  /**
  Control-flow [operator](#highlight.tags.operator).
  */
  controlOperator: t(operator),
  /**
  Program or markup punctuation.
  */
  punctuation,
  /**
  [Punctuation](#highlight.tags.punctuation) that separates
  things.
  */
  separator: t(punctuation),
  /**
  Bracket-style [punctuation](#highlight.tags.punctuation).
  */
  bracket,
  /**
  Angle [brackets](#highlight.tags.bracket) (usually `<` and `>`
  tokens).
  */
  angleBracket: t(bracket),
  /**
  Square [brackets](#highlight.tags.bracket) (usually `[` and `]`
  tokens).
  */
  squareBracket: t(bracket),
  /**
  Parentheses (usually `(` and `)` tokens). Subtag of
  [bracket](#highlight.tags.bracket).
  */
  paren: t(bracket),
  /**
  Braces (usually `{` and `}` tokens). Subtag of
  [bracket](#highlight.tags.bracket).
  */
  brace: t(bracket),
  /**
  Content, for example plain text in XML or markup documents.
  */
  content,
  /**
  [Content](#highlight.tags.content) that represents a heading.
  */
  heading,
  /**
  A level 1 [heading](#highlight.tags.heading).
  */
  heading1: t(heading),
  /**
  A level 2 [heading](#highlight.tags.heading).
  */
  heading2: t(heading),
  /**
  A level 3 [heading](#highlight.tags.heading).
  */
  heading3: t(heading),
  /**
  A level 4 [heading](#highlight.tags.heading).
  */
  heading4: t(heading),
  /**
  A level 5 [heading](#highlight.tags.heading).
  */
  heading5: t(heading),
  /**
  A level 6 [heading](#highlight.tags.heading).
  */
  heading6: t(heading),
  /**
  A prose [content](#highlight.tags.content) separator (such as a horizontal rule).
  */
  contentSeparator: t(content),
  /**
  [Content](#highlight.tags.content) that represents a list.
  */
  list: t(content),
  /**
  [Content](#highlight.tags.content) that represents a quote.
  */
  quote: t(content),
  /**
  [Content](#highlight.tags.content) that is emphasized.
  */
  emphasis: t(content),
  /**
  [Content](#highlight.tags.content) that is styled strong.
  */
  strong: t(content),
  /**
  [Content](#highlight.tags.content) that is part of a link.
  */
  link: t(content),
  /**
  [Content](#highlight.tags.content) that is styled as code or
  monospace.
  */
  monospace: t(content),
  /**
  [Content](#highlight.tags.content) that has a strike-through
  style.
  */
  strikethrough: t(content),
  /**
  Inserted text in a change-tracking format.
  */
  inserted: t(),
  /**
  Deleted text.
  */
  deleted: t(),
  /**
  Changed text.
  */
  changed: t(),
  /**
  An invalid or unsyntactic element.
  */
  invalid: t(),
  /**
  Metadata or meta-instruction.
  */
  meta,
  /**
  [Metadata](#highlight.tags.meta) that applies to the entire
  document.
  */
  documentMeta: t(meta),
  /**
  [Metadata](#highlight.tags.meta) that annotates or adds
  attributes to a given syntactic element.
  */
  annotation: t(meta),
  /**
  Processing instruction or preprocessor directive. Subtag of
  [meta](#highlight.tags.meta).
  */
  processingInstruction: t(meta),
  /**
  [Modifier](#highlight.Tag^defineModifier) that indicates that a
  given element is being defined. Expected to be used with the
  various [name](#highlight.tags.name) tags.
  */
  definition: Tag.defineModifier("definition"),
  /**
  [Modifier](#highlight.Tag^defineModifier) that indicates that
  something is constant. Mostly expected to be used with
  [variable names](#highlight.tags.variableName).
  */
  constant: Tag.defineModifier("constant"),
  /**
  [Modifier](#highlight.Tag^defineModifier) used to indicate that
  a [variable](#highlight.tags.variableName) or [property
  name](#highlight.tags.propertyName) is being called or defined
  as a function.
  */
  function: Tag.defineModifier("function"),
  /**
  [Modifier](#highlight.Tag^defineModifier) that can be applied to
  [names](#highlight.tags.name) to indicate that they belong to
  the language's standard environment.
  */
  standard: Tag.defineModifier("standard"),
  /**
  [Modifier](#highlight.Tag^defineModifier) that indicates a given
  [names](#highlight.tags.name) is local to some scope.
  */
  local: Tag.defineModifier("local"),
  /**
  A generic variant [modifier](#highlight.Tag^defineModifier) that
  can be used to tag language-specific alternative variants of
  some common tag. It is recommended for themes to define special
  forms of at least the [string](#highlight.tags.string) and
  [variable name](#highlight.tags.variableName) tags, since those
  come up a lot.
  */
  special: Tag.defineModifier("special")
};
for (let name2 in tags) {
  let val = tags[name2];
  if (val instanceof Tag)
    val.name = name2;
}
var classHighlighter = tagHighlighter([
  { tag: tags.link, class: "tok-link" },
  { tag: tags.heading, class: "tok-heading" },
  { tag: tags.emphasis, class: "tok-emphasis" },
  { tag: tags.strong, class: "tok-strong" },
  { tag: tags.keyword, class: "tok-keyword" },
  { tag: tags.atom, class: "tok-atom" },
  { tag: tags.bool, class: "tok-bool" },
  { tag: tags.url, class: "tok-url" },
  { tag: tags.labelName, class: "tok-labelName" },
  { tag: tags.inserted, class: "tok-inserted" },
  { tag: tags.deleted, class: "tok-deleted" },
  { tag: tags.literal, class: "tok-literal" },
  { tag: tags.string, class: "tok-string" },
  { tag: tags.number, class: "tok-number" },
  { tag: [tags.regexp, tags.escape, tags.special(tags.string)], class: "tok-string2" },
  { tag: tags.variableName, class: "tok-variableName" },
  { tag: tags.local(tags.variableName), class: "tok-variableName tok-local" },
  { tag: tags.definition(tags.variableName), class: "tok-variableName tok-definition" },
  { tag: tags.special(tags.variableName), class: "tok-variableName2" },
  { tag: tags.definition(tags.propertyName), class: "tok-propertyName tok-definition" },
  { tag: tags.typeName, class: "tok-typeName" },
  { tag: tags.namespace, class: "tok-namespace" },
  { tag: tags.className, class: "tok-className" },
  { tag: tags.macroName, class: "tok-macroName" },
  { tag: tags.propertyName, class: "tok-propertyName" },
  { tag: tags.operator, class: "tok-operator" },
  { tag: tags.comment, class: "tok-comment" },
  { tag: tags.meta, class: "tok-meta" },
  { tag: tags.invalid, class: "tok-invalid" },
  { tag: tags.punctuation, class: "tok-punctuation" }
]);

// node_modules/@lezer/cpp/dist/index.js
var RawString = 1;
var templateArgsEndFallback = 2;
var MacroName = 3;
var R = 82;
var L = 76;
var u = 117;
var U = 85;
var a = 97;
var z = 122;
var A = 65;
var Z = 90;
var Underscore = 95;
var Zero = 48;
var Quote = 34;
var ParenL = 40;
var ParenR = 41;
var Space = 32;
var GreaterThan = 62;
var rawString = new ExternalTokenizer((input) => {
  if (input.next == L || input.next == U) {
    input.advance();
  } else if (input.next == u) {
    input.advance();
    if (input.next == Zero + 8) input.advance();
  }
  if (input.next != R) return;
  input.advance();
  if (input.next != Quote) return;
  input.advance();
  let marker = "";
  while (input.next != ParenL) {
    if (input.next == Space || input.next <= 13 || input.next == ParenR) return;
    marker += String.fromCharCode(input.next);
    input.advance();
  }
  input.advance();
  for (; ; ) {
    if (input.next < 0)
      return input.acceptToken(RawString);
    if (input.next == ParenR) {
      let match = true;
      for (let i = 0; match && i < marker.length; i++)
        if (input.peek(i + 1) != marker.charCodeAt(i)) match = false;
      if (match && input.peek(marker.length + 1) == Quote)
        return input.acceptToken(RawString, 2 + marker.length);
    }
    input.advance();
  }
});
var fallback = new ExternalTokenizer((input) => {
  if (input.next == GreaterThan) {
    if (input.peek(1) == GreaterThan)
      input.acceptToken(templateArgsEndFallback, 1);
  } else {
    let sawLetter = false, i = 0;
    for (; ; i++) {
      if (input.next >= A && input.next <= Z) sawLetter = true;
      else if (input.next >= a && input.next <= z) return;
      else if (input.next != Underscore && !(input.next >= Zero && input.next <= Zero + 9)) break;
      input.advance();
    }
    if (sawLetter && i > 1) input.acceptToken(MacroName);
  }
}, { extend: true });
var cppHighlighting = styleTags({
  "typedef struct union enum class typename decltype auto template operator friend noexcept namespace using requires concept import export module __attribute__ __declspec __based": tags.definitionKeyword,
  "extern MsCallModifier MsPointerModifier extern static register thread_local inline const volatile restrict _Atomic mutable constexpr constinit consteval virtual explicit VirtualSpecifier Access": tags.modifier,
  "if else switch for while do case default return break continue goto throw try catch": tags.controlKeyword,
  "co_return co_yield co_await": tags.controlKeyword,
  "new sizeof delete static_assert": tags.operatorKeyword,
  "NULL nullptr": tags.null,
  this: tags.self,
  "True False": tags.bool,
  "TypeSize PrimitiveType": tags.standard(tags.typeName),
  TypeIdentifier: tags.typeName,
  FieldIdentifier: tags.propertyName,
  "CallExpression/FieldExpression/FieldIdentifier": tags.function(tags.propertyName),
  "ModuleName/Identifier": tags.namespace,
  "PartitionName": tags.labelName,
  StatementIdentifier: tags.labelName,
  "Identifier DestructorName": tags.variableName,
  "CallExpression/Identifier": tags.function(tags.variableName),
  "CallExpression/ScopedIdentifier/Identifier": tags.function(tags.variableName),
  "FunctionDeclarator/Identifier FunctionDeclarator/DestructorName": tags.function(tags.definition(tags.variableName)),
  NamespaceIdentifier: tags.namespace,
  OperatorName: tags.operator,
  ArithOp: tags.arithmeticOperator,
  LogicOp: tags.logicOperator,
  BitOp: tags.bitwiseOperator,
  CompareOp: tags.compareOperator,
  AssignOp: tags.definitionOperator,
  UpdateOp: tags.updateOperator,
  LineComment: tags.lineComment,
  BlockComment: tags.blockComment,
  Number: tags.number,
  String: tags.string,
  "RawString SystemLibString": tags.special(tags.string),
  CharLiteral: tags.character,
  EscapeSequence: tags.escape,
  "UserDefinedLiteral/Identifier": tags.literal,
  PreProcArg: tags.meta,
  "PreprocDirectiveName #include #ifdef #ifndef #if #define #else #endif #elif": tags.processingInstruction,
  MacroName: tags.special(tags.name),
  "( )": tags.paren,
  "[ ]": tags.squareBracket,
  "{ }": tags.brace,
  "< >": tags.angleBracket,
  ". ->": tags.derefOperator,
  ", ;": tags.separator
});
var spec_identifier = { __proto__: null, bool: 36, char: 36, int: 36, float: 36, double: 36, void: 36, size_t: 36, ssize_t: 36, intptr_t: 36, uintptr_t: 36, charptr_t: 36, int8_t: 36, int16_t: 36, int32_t: 36, int64_t: 36, uint8_t: 36, uint16_t: 36, uint32_t: 36, uint64_t: 36, char8_t: 36, char16_t: 36, char32_t: 36, char64_t: 36, const: 70, volatile: 72, restrict: 74, _Atomic: 76, mutable: 78, constexpr: 80, constinit: 82, consteval: 84, struct: 88, __declspec: 92, final: 148, override: 148, public: 152, private: 152, protected: 152, virtual: 154, extern: 160, static: 162, register: 164, inline: 166, thread_local: 168, __attribute__: 172, __based: 178, __restrict: 180, __uptr: 180, __sptr: 180, _unaligned: 180, __unaligned: 180, noexcept: 194, requires: 198, TRUE: 784, true: 784, FALSE: 786, false: 786, typename: 218, class: 220, template: 234, throw: 248, __cdecl: 256, __clrcall: 256, __stdcall: 256, __fastcall: 256, __thiscall: 256, __vectorcall: 256, try: 260, catch: 264, export: 282, import: 286, case: 296, default: 298, if: 308, else: 314, switch: 318, do: 322, while: 324, for: 330, return: 334, break: 338, continue: 342, goto: 346, co_return: 350, co_yield: 354, using: 362, typedef: 366, namespace: 380, new: 398, delete: 400, co_await: 402, concept: 406, enum: 410, static_assert: 414, friend: 422, union: 424, explicit: 430, operator: 444, module: 456, signed: 518, unsigned: 518, long: 518, short: 518, decltype: 528, auto: 530, sizeof: 566, NULL: 572, nullptr: 586, this: 588 };
var spec_ = { __proto__: null, "<": 131 };
var spec_templateArgsEnd = { __proto__: null, ">": 135 };
var spec_scopedIdentifier = { __proto__: null, operator: 388, new: 576, delete: 582 };
var parser = LRParser.deserialize({
  version: 14,
  states: "$:|Q!QQVOOP'gOUOOO(XOWO'#CdO,RQUO'#CgO,]QUO'#FjO-sQbO'#CxO.UQUO'#CxO0TQUO'#KZO0[QUO'#CwO0gOpO'#DvO0oQ!dO'#D]OOQR'#JO'#JOO5XQVO'#GUO5fQUO'#JVOOQQ'#JV'#JVO8zQUO'#KmO<eQUO'#KmO>{QVO'#E^O?]QUO'#E^OOQQ'#Ed'#EdOOQQ'#Ee'#EeO?bQVO'#EfO@XQVO'#EiOBUQUO'#FPOBvQUO'#FhOOQR'#Fj'#FjOB{QUO'#FjOOQR'#LQ'#LQOOQR'#LP'#LPOETQVO'#KQOFxQUO'#LVOGVQUO'#KqOGkQUO'#LVOH]QUO'#LXOOQR'#HU'#HUOOQR'#HV'#HVOOQR'#HW'#HWOOQR'#K|'#K|OOQR'#J_'#J_Q!QQVOOOHkQVO'#FOOIWQUO'#EhOI_QUOOOKZQVO'#HgOKkQUO'#HgONVQUO'#KqONaQUO'#KqOOQQ'#Kq'#KqO!!_QUO'#KqOOQQ'#Jq'#JqO!!lQUO'#HxOOQQ'#KZ'#KZO!&^QUO'#KZO!&zQUO'#KQO!(zQVO'#I]O!(zQVO'#I`OCQQUO'#KQOOQQ'#Ip'#IpOOQQ'#KQ'#KQO!,}QUO'#KZOOQR'#KY'#KYO!-UQUO'#DZO!/mQUO'#KnOOQQ'#Kn'#KnO!/tQUO'#KnO!/{QUO'#ETO!0QQUO'#EWO!0VQUO'#FRO8zQUO'#FPO!QQVO'#F^O!0[Q#vO'#F`O!0gQUO'#FkO!0oQUO'#FpO!0tQVO'#FrO!0oQUO'#FuO!3sQUO'#FvO!3xQVO'#FxO!4SQUO'#FzO!4XQUO'#F|O!4^QUO'#GOO!4cQVO'#GQO!(zQVO'#GSO!4jQUO'#GpO!4xQUO'#GYO!(zQVO'#FeO!6VQUO'#FeO!6[QVO'#G`O!6cQUO'#GaO!6nQUO'#GnO!6sQUO'#GrO!6xQUO'#GzO!7jQ&lO'#HiO!:mQUO'#GuO!:}QUO'#HXO!;YQUO'#HZO!;bQUO'#DXO!;bQUO'#HuO!;bQUO'#HvO!;yQUO'#HwO!<[QUO'#H|O!=PQUO'#H}O!>uQVO'#IbO!(zQVO'#IdO!?PQUO'#IgO!?WQVO'#IjP!@}{,UO'#CbP!6n{,UO'#CbP!AY{7[O'#CbP!6n{,UO'#CbP!A_{,UO'#CbP!AjOSO'#IzPOOO)CEn)CEnOOOO'#I|'#I|O!AtOWO,59OOOQR,59O,59OO!(zQVO,59VOOQQ,59X,59XO!(zQVO,5;ROOQR,5<U,5<UO!BPQUO,59ZO!(zQVO,5>qOOQR'#IX'#IXOOQR'#IY'#IYOOQR'#IZ'#IZOOQR'#I['#I[O!(zQVO,5>rO!(zQVO,5>rO!(zQVO,5>rO!(zQVO,5>rO!(zQVO,5>rO!(zQVO,5>rO!(zQVO,5>rO!(zQVO,5>rO!(zQVO,5>rO!(zQVO,5>rO!DOQVO,5>zOOQQ,5?W,5?WO!EqQVO'#CjO!IjQUO'#CzOOQQ,59d,59dOOQQ,59c,59cOOQQ,5<},5<}O!IwQ&lO,5=mO!?PQUO,5?RO!LkQVO,5?UO!LrQbO,59dO!L}QVO'#FYOOQQ,5?P,5?PO!M_QVO,59WO!MfO`O,5:bO!MkQbO'#D^O!M|QbO'#K_O!N[QbO,59wO!NdQbO'#CxO!NuQUO'#CxO!NzQUO'#KZO# UQUO'#CwOOQR-E<|-E<|O# aQUO,5AoO# hQVO'#EfO@XQVO'#EiOBUQUO,5;kOOQR,5<p,5<pO#$aQUO'#KQO#$hQUO'#KQO!(zQVO'#IUO8zQUO,5;kO#${Q&lO'#HiO#(SQUO'#CtO#*wQbO'#CxO#*|QUO'#CwO#.jQUO'#KZOOQQ-E=T-E=TO#0}QUO,5AXO#1XQUO'#KZO#1cQUO,5AXOOQR,5Ao,5AoOOQQ,5>l,5>lO#3gQUO'#CgO#4]QUO,5>pO#6OQUO'#IeOOQR'#I}'#I}O#6WQUO,5:xO#6tQUO,5:xO#7eQUO,5:xO#8YQUO'#CuO!0QQUO'#CmOOQQ'#JW'#JWO#6tQUO,5:xO#8bQUO,5;QO!4xQUO'#DOO#9kQUO,5;QO#9pQUO,5>QO#:|QUO'#DOO#;dQUO,5>{O#;iQUO'#KwO#<rQUO,5;TO#<zQVO,5;TO#=UQUO,5;TOOQQ,5;T,5;TO#>}QUO'#L[O#?UQUO,5>UO#?ZQbO'#CxO#?fQUO'#GcO#?kQUO'#E^O#@[QUO,5;kO#@sQUO'#K}O#@{QUO,5;rOKkQUO'#HfOBUQUO'#HgO#AQQUO'#KqO!6nQUO'#HjO#AxQUO'#CuO!0tQVO,5<SOOQQ'#Cg'#CgOOQR'#Jh'#JhO#A}QVO,5=`OOQQ,5?Z,5?ZO#DWQbO'#CxO#DcQUO'#GcOOQQ'#Ji'#JiOOQQ-E=g-E=gOGVQUO,5AqOGkQUO,5AqO#DhQUO,5AsO#DsQUO'#G|OOQR,5Aq,5AqO#DhQUO,5AqO#EOQUO'#HOO#EWQUO,5AsOOQR,5As,5AsOOQR,5At,5AtO#EfQVO,5AtOOQR-E=]-E=]O#G`QVO,5;jOOQR,5;j,5;jO#IaQUO'#EjO#JfQUO'#EwO#K]QVO'#ExO#MoQUO'#EvO#MwQUO'#EyO#NvQUO'#EzOOQQ'#Kz'#KzO$ mQUO,5;SO$!sQUO'#EvOOQQ,5;S,5;SO$#pQUO,5;SO$%cQUO,5:yO$'|QVO,5>PO$(WQUO'#E[O$(eQUO,5>ROOQQ,5>S,5>SO$,RQVO'#C|OOQQ-E=o-E=oOOQQ,5>d,5>dOOQQ,59a,59aO$,]QUO,5>wO$.]QUO,5>zO!6nQUO,59uO$.pQUO,5;qO$.}QUO,5<{O!0QQUO,5:oOOQQ,5:r,5:rO$/YQUO,5;mO$/_QUO'#KmOBUQUO,5;kOOQR,5;x,5;xO$0OQUO'#FbO$0^QUO'#FbO$0cQUO,5;zO$3|QVO'#FmO!0tQVO,5<VO!0oQUO,5<VO!0VQUO,5<[O$4TQVO'#GUO$7PQUO,5<^O!0tQVO,5<aO$:gQVO,5<bO$:tQUO,5<dOOQR,5<d,5<dO$;}QUO,5<dOOQR,5<f,5<fOOQR,5<h,5<hOOQQ'#Fi'#FiO$<SQUO,5<jO$<XQUO,5<lOOQR,5<l,5<lO$=_QUO,5<nO$>eQUO,5<rO$>pQUO,5=[O$>uQUO,5=[O!4xQUO,5<tO$>}QUO,5<tO$?cQUO,5<PO$@iQVO,5<PO$BzQUO,5<zOOQR,5<z,5<zOOQR,5<{,5<{O$>uQUO,5<{O$DQQUO,5<{O$D]QUO,5=YO!(zQVO,5=^O!(zQVO,5=fO#NeQUO,5=mOOQQ,5>T,5>TO$FbQUO,5>TO$FlQUO,5>TO$FqQUO,5>TO$FvQUO,5>TO!6nQUO,5>TO$HtQUO'#KZO$H{QUO,5=oO$IWQUO,5=aOKkQUO,5=oO$JQQUO,5=sOOQR,5=s,5=sO$JYQUO,5=sO$LeQVO'#H[OOQQ,5=u,5=uO!;]QUO,5=uO%#`QUO'#KjO%#gQUO'#K[O%#{QUO'#KjO%$VQUO'#DyO%$hQUO'#D|O%'eQUO'#K[OOQQ'#K['#K[O%)WQUO'#K[O%#gQUO'#K[O%)]QUO'#K[OOQQ,59s,59sOOQQ,5>a,5>aOOQQ,5>b,5>bO%)eQUO'#HzO%)mQUO,5>cOOQQ,5>c,5>cO%-XQUO,5>cO%-dQUO,5>hO%1OQVO,5>iO%1VQUO,5>|O# hQVO'#EfO%4]QUO,5>|OOQQ,5>|,5>|O%4|QUO,5?OO%7QQUO,5?RO!<[QUO,5?RO%8|QUO,5?UO%<iQVO,5?UP!A_{,UO,58|P%<p{,UO,58|P%=O{7[O,58|P%=U{,UO,58|PO{O'#Ju'#JuP%=Z{,UO'#LcPOOO'#Lc'#LcP%=a{,UO'#LcPOOO,58|,58|POOO,5?f,5?fP%=fOSO,5?fOOOO-E<z-E<zOOQR1G.j1G.jO%=mQUO1G.qO%>sQUO1G0mOOQQ1G0m1G0mO%@PQUO'#CpO%B`QbO'#CxO%BkQUO'#CsO%BpQUO'#CsO%BuQUO1G.uO#AxQUO'#CrOOQQ1G.u1G.uO%DxQUO1G4]O%FOQUO1G4^O%GqQUO1G4^O%IdQUO1G4^O%KVQUO1G4^O%LxQUO1G4^O%NkQUO1G4^O&!^QUO1G4^O&$PQUO1G4^O&%rQUO1G4^O&'eQUO1G4^O&)WQUO1G4^O&*yQUO'#KPO&,SQUO'#KPO&,[QUO,59UOOQQ,5=P,5=PO&.dQUO,5=PO&.nQUO,5=PO&.sQUO,5=PO&.xQUO,5=PO!6nQUO,5=PO#NeQUO1G3XO&/SQUO1G4mO!<[QUO1G4mO&1OQUO1G4pO&2qQVO1G4pOOQQ1G/O1G/OOOQQ1G.}1G.}OOQQ1G2i1G2iO!IwQ&lO1G3XO&2xQUO'#LOO@XQVO'#EiO&4RQUO'#F]OOQQ'#Ja'#JaO&4WQUO'#FZO&4cQUO'#LOO&4kQUO,5;tO&4pQUO1G.rOOQQ1G.r1G.rOOQR1G/|1G/|O&6cQ!dO'#JPO&6hQbO,59xO&8yQ!eO'#D`O&9QQ!dO'#JRO&9VQbO,5@yO&9VQbO,5@yOOQR1G/c1G/cO&9bQbO1G/cO&9gQ&lO'#GeO&:eQbO,59dOOQR1G7Z1G7ZO#@[QUO1G1VO&:pQUO1G1^OBUQUO1G1VO&=RQUO'#CzO#*wQbO,59dO&@tQUO1G6sOOQR-E<{-E<{O&BWQUO1G0dO#6WQUO1G0dOOQQ-E=U-E=UO#6tQUO1G0dOOQQ1G0l1G0lO&B{QUO,59jOOQQ1G3l1G3lO&CcQUO,59jO&CyQUO,59jO!M_QVO1G4gO!(zQVO'#JYO&DeQUO,5AcOOQQ1G0o1G0oO!(zQVO1G0oO!6nQUO'#JnO&DmQUO,5AvOOQQ1G3p1G3pOOQR1G1V1G1VO&HjQVO'#FOO!M_QVO,5;sOOQQ,5;s,5;sOBUQUO'#JcO&JfQUO,5AiO&JnQVO'#E[OOQR1G1^1G1^O&M]QUO'#L[OOQR1G1n1G1nOOQR-E=f-E=fOOQR1G7]1G7]O#DhQUO1G7]OGVQUO1G7]O#DhQUO1G7_OOQR1G7_1G7_O&MeQUO'#G}O&MmQUO'#LWOOQQ,5=h,5=hO&M{QUO,5=jO&NQQUO,5=kOOQR1G7`1G7`O#EfQVO1G7`O&NVQUO1G7`O' ]QVO,5=kOOQR1G1U1G1UO$.vQUO'#E]O'!RQUO'#E]OOQQ'#Ky'#KyO'!lQUO'#KxO'!wQUO,5;UO'#PQUO'#ElO'#dQUO'#ElO'#wQUO'#EtOOQQ'#J['#J[O'#|QUO,5;cO'$sQUO,5;cO'%nQUO,5;dO'&tQVO,5;dOOQQ,5;d,5;dO''OQVO,5;dO'&tQVO,5;dO''VQUO,5;bO'(SQUO,5;eO'(_QUO'#KpO'(gQUO,5:vO'(lQUO,5;fOOQQ1G0n1G0nOOQQ'#J]'#J]O''VQUO,5;bO!4xQUO'#E}OOQQ,5;b,5;bO')gQUO'#E`O'+aQUO'#E{OHrQUO1G0nO'+fQUO'#EbOOQQ'#JX'#JXO'-OQUO'#KrOOQQ'#Kr'#KrO'-xQUO1G0eO'.pQUO1G3kO'/vQVO1G3kOOQQ1G3k1G3kO'0QQVO1G3kO'0XQUO'#L_O'1eQUO'#KXO'1sQUO'#KWO'2OQUO,59hO'2WQUO1G/aO'2]QUO'#FPOOQR1G1]1G1]OOQR1G2g1G2gO$>uQUO1G2gO'2gQUO1G2gO'2rQUO1G0ZOOQR'#J`'#J`O'2wQVO1G1XO'8pQUO'#FTO'8uQUO1G1VO!6nQUO'#JdO'9TQUO,5;|O$0^QUO,5;|OOQQ'#Fc'#FcOOQQ,5;|,5;|O'9cQUO1G1fOOQR1G1f1G1fO'9kQUO,5<XO$.vQUO'#FWOBUQUO'#FWO'9rQUO,5<XO!(zQVO,5<XO'9zQUO,5<XO':PQVO1G1qO!0tQVO1G1qOOQR1G1v1G1vO'?oQUO1G1xOOQR1G1{1G1{O'?tQUO1G1|OBUQUO1G2]O'@}QVO1G1|O'CcQUO1G1|O'ChQUO'#GWO8zQUO1G2]OOQR1G2O1G2OOOQR1G2U1G2UOOQR1G2W1G2WOOQR1G2Y1G2YO'CmQUO1G2^O!4xQUO1G2^OOQR1G2v1G2vO'CuQUO1G2vO$>}QUO1G2`OOQQ'#Cv'#CvO'CzQUO'#G[O'DuQUO'#G[O'DzQUO'#LRO'EYQUO'#G_OOQQ'#LS'#LSO'EhQUO1G2`O'EmQVO1G1kO'HOQVO'#GUOBUQUO'#FWOOQR'#Je'#JeO'EmQVO1G1kO'HYQUO'#FvOOQR1G2f1G2fO'H_QUO1G2gO'HdQUO'#JgO'2gQUO1G2gO!(zQVO1G2tO'HlQUO1G2xO'IuQUO1G3QO'J{QUO1G3XOOQQ1G3o1G3oO'KaQUO1G3oOOQR1G3Z1G3ZO'KfQUO'#KZO'2]QUO'#LTOGkQUO'#LVOOQR'#Gy'#GyO#DhQUO'#LXOOQR'#HQ'#HQO'KpQUO'#GvO'#wQUO'#GuOOQR1G2{1G2{O'LmQUO1G2{O'MdQUO1G3ZO'MoQUO1G3_O'MtQUO1G3_OOQR1G3_1G3_O'M|QUO'#H]OOQR'#H]'#H]O( VQUO'#H]O!(zQVO'#H`O!(zQVO'#H_OOQR'#LZ'#LZO( [QUO'#LZOOQR'#Jk'#JkO( aQVO,5=vOOQQ,5=v,5=vO( hQUO'#H^O( pQUO'#HZOOQQ1G3a1G3aO( zQUO,5@vOOQQ,5@v,5@vO%)WQUO,5@vO%)]QUO,5@vO%$VQUO,5:eO(%iQUO'#KkO(%wQUO'#KkOOQQ,5:e,5:eOOQQ'#JS'#JSO(&SQUO'#D}O(&^QUO'#KqOGkQUO'#LVO('YQUO'#D}OOQQ'#Hp'#HpOOQQ'#Hr'#HrOOQQ'#Hs'#HsOOQQ'#Kl'#KlOOQQ'#JU'#JUO('dQUO,5:hOOQQ,5:h,5:hO((aQUO'#LVO((nQUO'#HtO()UQUO,5@vO()]QUO'#H{O()hQUO'#L^O()pQUO,5>fO()uQUO'#L]OOQQ1G3}1G3}O(-lQUO1G3}O(-sQUO1G3}O(-zQUO1G4TO(/QQUO1G4TO(/VQUO,5A|O!6nQUO1G4hO!(zQVO'#IiOOQQ1G4m1G4mO(/[QUO1G4mO(1_QVO1G4pPOOO1G.h1G.hP!A_{,UO1G.hP(3_QUO'#LeP(3j{,UO1G.hP(3o{7[O1G.hPO{O-E=s-E=sPOOO,5A},5A}P(3w{,UO,5A}POOO1G5Q1G5QO!(zQVO7+$]O(3|QUO'#CzOOQQ,59_,59_O(4XQbO,59dO(4dQbO,59_OOQQ,59^,59^OOQQ7+)w7+)wO!M_QVO'#JtO(4oQUO,5@kOOQQ1G.p1G.pOOQQ1G2k1G2kO(4wQUO1G2kO(4|QUO7+(sOOQQ7+*X7+*XO(7bQUO7+*XO(7iQUO7+*XO(1_QVO7+*[O#NeQUO7+(sO(7vQVO'#JbO(8ZQUO,5AjO(8cQUO,5;vOOQQ'#Cp'#CpOOQQ,5;w,5;wO!(zQVO'#F[OOQQ-E=_-E=_O!M_QVO,5;uOOQQ1G1`1G1`OOQQ,5?k,5?kOOQQ-E<}-E<}OOQR'#Dg'#DgOOQR'#Di'#DiOOQR'#Dl'#DlO(9lQ!eO'#K`O(9sQMkO'#K`O(9zQ!eO'#K`OOQR'#K`'#K`OOQR'#JQ'#JQO(:RQ!eO,59zOOQQ,59z,59zO(:YQbO,5?mOOQQ-E=P-E=PO(:hQbO1G6eOOQR7+$}7+$}OOQR7+&q7+&qOOQR7+&x7+&xO'8uQUO7+&qO(:sQUO7+&OO#6WQUO7+&OO(;hQUO1G/UO(<OQUO1G/UO(<jQUO7+*ROOQQ7+*V7+*VO(>]QUO,5?tOOQQ-E=W-E=WO(?fQUO7+&ZOOQQ,5@Y,5@YOOQQ-E=l-E=lO(?kQUO'#LOO@XQVO'#EiO(@wQUO1G1_OOQQ1G1_1G1_O(BQQUO,5?}OOQQ,5?},5?}OOQQ-E=a-E=aO(BfQUO'#KpOOQR7+,w7+,wO#DhQUO7+,wOOQR7+,y7+,yO(BsQUO,5=iO#DsQUO'#JjO(CUQUO,5ArOOQR1G3U1G3UOOQR1G3V1G3VO(CdQUO7+,zOOQR7+,z7+,zO(E[QUO,5:wO(FyQUO'#EwO!(zQVO,5;VO(GlQUO,5:wO(GvQUO'#EpO(HXQUO'#EzOOQQ,5;Z,5;ZO#K]QVO'#ExO(HoQUO,5:wO(HvQUO'#EyO#GgQUO'#JZO(J`QUO,5AdOOQQ1G0p1G0pO(JkQUO,5;WO!<[QUO,5;^O(KUQUO,5;_O(KdQUO,5;WO(MvQUO,5;`OOQQ-E=Y-E=YO(NOQUO1G0}OOQQ1G1O1G1OO(NyQUO1G1OO)!PQVO1G1OO)!WQVO1G1OO)!bQUO1G0|OOQQ1G0|1G0|OOQQ1G1P1G1PO)#_QUO'#JoO)#iQUO,5A[OOQQ1G0b1G0bOOQQ-E=Z-E=ZO)#qQUO,5;iO!<[QUO,5;iO)$nQVO,5:zO)$uQUO,5;gO$ mQUO7+&YOOQQ7+&Y7+&YO!(zQVO'#EfO)$|QUO,5:|OOQQ'#Ks'#KsOOQQ-E=V-E=VOOQQ,5A^,5A^OOQQ'#Jl'#JlO)(qQUO7+&PPOQQ7+&P7+&POOQQ7+)V7+)VO))iQUO7+)VO)*oQVO7+)VOOQQ,5>m,5>mO$)YQVO'#JsO)*vQUO,5@rOOQQ1G/S1G/SOOQQ7+${7+${O)+RQUO7+(RO)+WQUO7+(ROOQR7+(R7+(RO$>uQUO7+(ROOQQ7+%u7+%uOOQR-E=^-E=^O!0VQUO,5;oOOQQ,5@O,5@OOOQQ-E=b-E=bO$0^QUO1G1hOOQQ1G1h1G1hOOQR7+'Q7+'QOOQR1G1s1G1sOBUQUO,5;rO)+tQUO,5<YO)+{QUO1G1sO)-UQUO1G1sO!0tQVO7+']O)-ZQVO7+']O)2yQUO7+'dO)3OQVO7+'hO)5dQUO7+'wO)5nQUO7+'hO)6tQVO7+'hOKkQUO7+'wO$>hQUO,5<rO!4xQUO7+'xO)6{QUO7+'xOOQR7+(b7+(bO)7QQUO7+'zO)7VQUO,5<vO'CzQUO,5<vO)7}QUO,5<vO'CzQUO,5<vOOQQ,5<w,5<wO)8`QVO,5<xO'EYQUO'#JfO)8jQUO,5AmO)8rQUO,5<yOOQR7+'z7+'zO)8}QVO7+'VO)5gQUO'#K}OOQR-E=c-E=cO);`QVO,5<bOOQQ,5@R,5@RO!6nQUO,5@ROOQQ-E=e-E=eO)=wQUO7+(`O)>}QUO7+(dO)?SQVO7+(dOOQQ7+(l7+(lOOQQ7+)Z7+)ZO)?[QUO'#KjO)?fQUO'#KjOOQR,5=b,5=bO)?sQUO,5=bO!;bQUO,5=bO!;bQUO,5=bO!;bQUO,5=bOOQR7+(g7+(gOOQR7+(u7+(uOOQR7+(y7+(yOOQR,5=w,5=wO)?xQUO,5=zO)AOQUO,5=yOOQR,5Au,5AuOOQR-E=i-E=iOOQQ1G3b1G3bO)BUQUO,5=xO)BZQVO'#EfOOQQ1G6b1G6bO%)WQUO1G6bO%)]QUO1G6bOOQQ1G0P1G0POOQQ-E=Q-E=QO)DrQUO,5AVO(%iQUO'#JTO)D}QUO,5AVO)D}QUO,5AVO)EVQUO,5:iO8zQUO,5:iOOQQ,5>],5>]O)EaQUO,5AqO)EhQUO'#EVO)FrQUO'#EVO)G]QUO,5:iO)GgQUO'#HlO)GgQUO'#HmOOQQ'#Ko'#KoO)HUQUO'#KoO!(zQVO'#HnOOQQ,5:i,5:iO)HvQUO,5:iO!M_QVO,5:iOOQQ-E=S-E=SOOQQ1G0S1G0SOOQQ,5>`,5>`O)H{QUO1G6bO!(zQVO,5>gO)LjQUO'#JrO)LuQUO,5AxOOQQ1G4Q1G4QO)L}QUO,5AwOOQQ,5Aw,5AwOOQQ7+)i7+)iO*!lQUO7+)iOOQQ7+)o7+)oO*'kQVO1G7hO*)mQUO7+*SO*)rQUO,5?TO**xQUO7+*[POOO7+$S7+$SP*,kQUO'#LfP*,sQUO,5BPP*,x{,UO7+$SPOOO1G7i1G7iO*,}QUO<<GwOOQQ1G.y1G.yOOQQ'#IT'#ITO*.pQUO,5@`OOQQ,5@`,5@`OOQQ-E=r-E=rOOQQ7+(V7+(VOOQQ<<Ms<<MsO*/yQUO<<MsO*1|QUO<<MvO*3oQUO<<L_O*4TQUO,5?|OOQQ,5?|,5?|OOQQ-E=`-E=`OOQQ1G1b1G1bO*5^QUO,5;vO*6dQUO1G1aOOQQ1G1a1G1aOOQR,5@z,5@zO*7mQ!eO,5@zO*7tQMkO,5@zO*7{Q!eO,5@zOOQR-E=O-E=OOOQQ1G/f1G/fO*8SQ!eO'#DwOOQQ1G5X1G5XOOQR<<J]<<J]O*8ZQUO<<IjO*9OQUO7+$pOOQQ<<Iu<<IuO(7vQVO,5;ROOQR<=!c<=!cOOQQ1G3T1G3TOOQQ,5@U,5@UOOQQ-E=h-E=hOOQR<=!f<=!fO*9{QUO1G0cO*:SQUO'#EzO*:dQUO1G0cO*:kQUO'#I}O*<RQUO1G0qO!(zQVO1G0qOOQQ,5;[,5;[OOQQ,5;],5;]OOQQ,5?u,5?uOOQQ-E=X-E=XO!<[QUO1G0xO*=bQUO1G0xOOQQ1G0y1G0yO*=sQUO'#ElOOQQ1G0z1G0zOOQQ7+&j7+&jO*>XQUO7+&jO*?_QVO7+&jOOQQ7+&h7+&hOOQQ,5@Z,5@ZOOQQ-E=m-E=mO*@ZQUO1G1TO*@eQUO1G1TO*AOQUO1G0fOOQQ1G0f1G0fO*BUQUO'#K{O*B^QUO1G1ROOQQ<<It<<ItOOQQ'#Hb'#HbO'+fQUO,5={OOQQ'#Hd'#HdO'+fQUO,5=}OOQQ-E=j-E=jPOQQ<<Ik<<IkPOQQ-E=k-E=kOOQQ<<Lq<<LqO*BcQUO'#LaO*CoQUO'#L`OOQQ,5@_,5@_OOQQ-E=q-E=qOOQR<<Km<<KmO$>uQUO<<KmO*C}QUO<<KmOOQR1G1Z1G1ZOOQQ7+'S7+'SO!M_QVO1G1tO*DSQUO1G1tOOQR7+'_7+'_OOQR<<Jw<<JwO!0tQVO<<JwOOQR<<KO<<KOO*D_QUO<<KSO*EeQVO<<KSOKkQUO<<KcO!M_QVO<<KcO*ElQUO<<KSO!0tQVO<<KSO*FuQUO<<KSO*FzQUO<<KcO*GVQUO<<KdOOQR<<Kd<<KdOOQR<<Kf<<KfO*G[QUO1G2bO)7VQUO1G2bO'CzQUO1G2bO*GmQUO1G2dO*HsQVO1G2dOOQQ1G2d1G2dO*H}QVO1G2dO*IUQUO,5@QOOQQ-E=d-E=dOOQQ1G2e1G2eO*IdQUO1G1|O*JmQVO1G1|O*JtQUO1G1|OOQQ1G5m1G5mOOQR<<Kz<<KzOOQR<<LO<<LOO*JyQVO<<LOO*KUQUO<<LOOOQR1G2|1G2|O*KZQUO1G2|O*KbQUO1G3eOOQR1G3d1G3dOOQQ7++|7++|O%)WQUO7++|O*KmQUO1G6qO*KmQUO1G6qO(%iQUO,5?oO*KuQUO,5?oOOQQ-E=R-E=RO*LQQUO1G0TOOQQ1G0T1G0TO*L[QUO1G0TO!M_QVO1G0TO*LaQUO1G0TOOQQ1G3w1G3wO*LkQUO,5:qO)EhQUO,5:qO*MXQUO,5:qO)EhQUO,5:qO$#uQUO,5:uO*MvQVO,5>VO)GgQUO'#JpO*NQQUO1G0TO*NcQVO1G0TOOQQ1G3u1G3uO*NjQUO,5>WO*NuQUO,5>XO+ dQUO,5>YO+!jQUO1G0TO%)]QUO7++|O+#pQUO1G4ROOQQ,5@^,5@^OOQQ-E=p-E=pOOQQ<<MT<<MTOOQQ<<Mn<<MnO+$yQUO1G4oP+&|QUO'#JvP+'UQUO,5BQPO{O1G7k1G7kPOOO<<Gn<<GnOOQQANC_ANC_OOQR1G6f1G6fO+'^Q!eO,5:cOOQQ,5:c,5:cO+'eQUO1G0mO+(qQUO7+&]O+*QQUO7+&dO+*cQUO,5;WOOQQ<<JU<<JUO+*qQUO7+&oOOQQ7+&Q7+&QO!4xQUO'#J^O++lQUO,5AgOOQQ7+&m7+&mOOQQ1G3g1G3gO++tQUO1G3iOOQQ,5>n,5>nO+/iQUOANAXOOQRANAXANAXO+/nQUO7+'`OOQRAN@cAN@cO+0zQVOAN@nO+1RQUOAN@nO!0tQVOAN@nO+2[QUOAN@nO+2aQUOAN@}O+2lQUOAN@}O+3rQUOAN@}OOQRAN@nAN@nO!M_QVOAN@}OOQRANAOANAOO+3wQUO7+'|O)7VQUO7+'|OOQQ7+(O7+(OO+4YQUO7+(OO+5`QVO7+(OO+5gQVO7+'hO+5nQUOANAjOOQR7+(h7+(hOOQR7+)P7+)PO+5sQUO7+)PO+5xQUO7+)POOQQ<= h<= hO+6QQUO7+,]O+6YQUO1G5ZOOQQ1G5Z1G5ZO+6eQUO7+%oOOQQ7+%o7+%oO+6vQUO7+%oO*NcQVO7+%oOOQQ7+)a7+)aO+6{QUO7+%oO+8RQUO7+%oO!M_QVO7+%oO+8]QUO1G0]O*LkQUO1G0]O)EhQUO1G0]OOQQ1G0a1G0aO+8zQUO1G3qO+:QQVO1G3qOOQQ1G3q1G3qO+:[QVO1G3qO+:cQUO,5@[OOQQ-E=n-E=nOOQQ1G3r1G3rO%)WQUO<= hOOQQ7+*Z7+*ZPOQQ,5@b,5@bPOQQ-E=t-E=tOOQQ1G/}1G/}OOQQ,5?x,5?xOOQQ-E=[-E=[OOQRG26sG26sO+:zQUOG26YO!0tQVOG26YO+<TQUOG26YOOQRG26YG26YO!M_QVOG26iO!0tQVOG26iO+<YQUOG26iO+=`QUOG26iO+=eQUO<<KhOOQQ<<Kj<<KjOOQRG27UG27UOOQR<<Lk<<LkO+=vQUO<<LkOOQQ7+*u7+*uOOQQ<<IZ<<IZO+={QUO<<IZO!M_QVO<<IZO+>QQUO<<IZO+?WQUO<<IZO*NcQVO<<IZOOQQ<<L{<<L{O+?iQUO7+%wO*LkQUO7+%wOOQQ7+)]7+)]O+@WQUO7+)]O+A^QVO7+)]OOQQANESANESO!0tQVOLD+tOOQRLD+tLD+tO+AeQUOLD,TO+BkQUOLD,TOOQRLD,TLD,TO!0tQVOLD,TOOQRANBVANBVOOQQAN>uAN>uO+BpQUOAN>uO+CvQUOAN>uO!M_QVOAN>uO+C{QUO<<IcOOQQ<<Lw<<LwOOQR!$( `!$( `O!0tQVO!$( oOOQR!$( o!$( oOOQQG24aG24aO+DjQUOG24aO+EpQUOG24aOOQR!)9EZ!)9EZOOQQLD){LD){O+EuQUO'#CgO(dQUO'#CgO+IrQUO'#CzO+LcQUO'#CzO!E{QUO'#CzO+M[QUO'#CzO+MoQUO'#CzO,#bQUO'#CzO,#rQUO'#CzO,$PQUO'#CzO,$[QbO,59dO,$gQbO,59dO,$rQbO,59dO,$}QbO'#CxO,%`QbO'#CxO,%qQbO'#CxO,&SQUO'#CgO,(gQUO'#CgO,(tQUO'#CgO,+iQUO'#CgO,.lQUO'#CgO,.|QUO'#CgO,2uQUO'#CgO,2|QUO'#CgO,3|QUO'#CgO,6VQUO,5:xO#?kQUO,5:xO#?kQUO,5:xO#=ZQUO'#L[O,6sQbO'#CxO,7OQbO'#CxO,7ZQbO'#CxO,7fQbO'#CxO#6tQUO'#E^O,7qQUO'#E^O,9OQUO'#HgO,9pQbO'#CxO,9{QbO'#CxO,:WQUO'#CwO,:]QUO'#CwO,:bQUO'#CpO,:pQbO,59dO,:{QbO,59dO,;WQbO,59dO,;cQbO,59dO,;nQbO,59dO,;yQbO,59dO,<UQbO,59dO,6VQUO1G0dO,<aQUO1G0dO#?kQUO1G0dO,7qQUO1G0dO,>nQUO'#KZO,?OQUO'#CzO,?^QbO,59dO,6VQUO7+&OO,<aQUO7+&OO,?iQUO'#EwO,@[QUO'#EzO,@{QUO'#E^O,AQQUO'#GcO,AVQUO'#CwO,A[QUO'#CxO,AaQUO'#CxO,AfQUO'#CwO,AkQUO'#GcO,ApQUO'#KZO,B^QUO'#KZO,BhQUO'#CwO,BsQUO'#CwO,COQUO'#CwO,<aQUO,5:xO,7qQUO,5:xO,7qQUO,5:xO,CZQUO'#KZO,CnQbO'#CxO,CyQUO'#CsO,DOQUO'#E^",
  stateData: ",Dt~O(nOSSOSTOSRPQVPQ'ePQ'gPQ'hPQ'iPQ'jPQ'kPQ'lPQ'mPQ~O*ZOS~OPmO]eOb!]Oe!POmTOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO!O!_O!TxO!VfO!X!XO!Y!WO!i!YO!opO!r!`O!s!aO!t!aO!u!bO!v!aO!x!cO!{!dO#V#QO#a#VO#b#TO#i#OO#p!xO#t!fO#v!eO$R!gO$T!hO$Y!vO$Z!wO$`!iO$e!jO$g!kO$h!lO$k!mO$m!nO$o!oO$q!pO$s!qO$u!rO$w!sO${!tO$}!uO%U!yO%_#ZO%`#[O%a#YO%c!zO%e#UO%g!{O%l#SO%o!|O%v!}O%|#PO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(rRO)QYO)TaO)V|O)W{O)XiO)Y!ZO)[XO)hcO)idO~OR#bOV#]O'e#^O'g#_O'h#`O'i#`O'j#aO'k#aO'l#_O'm#_O~OX#dO(o#fO(q#dO~O]ZX]jXejXmhXqZXqjXsjXtjXujXvjXwjXxjXyjXzjX!OjX!TjX!VZX!VjX!XZX!YZX![ZX!^ZX!_ZX!aZX!bZX!cZX!eZX!fZX!gZX!hZX!rjX!sjX!tjX!ujX!vjX!xjX!{jX%vjX&rjX&sjX(rjX(uZX(v$]X(wZX(xZX)TZX)TjX)UZX)VZX)VjX)WZX)WjX)XZX)YZX)jZX~O)XjX!UZX~P(dO]#}O!V#lO!X#{O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO(w#kO(x#kO)T#mO)U#oO)V#nO)W#pO)X#jO)Y#|O~Oe$RO%Y$SO'[$TO'_$UO(y$OO~Om$VO~O!T$WO](}Xe(}Xs(}Xt(}Xu(}Xv(}Xw(}Xx(}Xy(}Xz(}X!O(}X!V(}X!r(}X!s(}X!t(}X!u(}X!v(}X!x(}X!{(}X%v(}X&r(}X&s(}X(r(}X)T(}X)V(}X)W(}X)X(}X~Om$VO~P.ZOm$VO!g$YO)j$YO~OX$ZO)]$ZO~O!R$[O)P)RP)Y)RP~OPmO]$eOb!]Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO!O!_O!TxO!V$fO!X!XO!Y!WO!i!YO!r!aO!s!aO!t!aO!u!aO!v!aO!x!cO#V#QO#a#VO#b#TO#v!eO$Y!vO$Z!wO$`!iO$e!jO$g!kO$h!lO$k!mO$m!nO$o!oO$q!pO$s!qO$u!rO$w!sO%_#ZO%`#[O%a#YO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO)QYO)T$kO)W$kO)XiO)Y!ZO)[XO)hcO)idO~Om$_O#t$lO(rRO~P0zO](]Xb'yXe(]Xm'yXm(]Xs'yXs(]Xt'yXt(]Xu'yXu(]Xv'yXv(]Xw'yXw(]Xx'yXx(]Xy'yXy(]Xz'yXz(]X|'yX!O'yX!V(]X!o(]X!r'yX!r(]X!s'yX!s(]X!t'yX!t(]X!u'yX!u(]X!v'yX!v(]X!x'yX!x(]X!{(]X#a'yX#b'yX%e'yX%l'yX%o(]X%v(]X&m'yX&r'yX&s'yX(r'yX(r(]X)T(]X)V(]X)W(]X~Ob!TOm$oOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO!O!_O!r!aO!s!aO!t!aO!u!aO!v!aO!x!cO#a#VO#b#TO%e#UO%l#SO&m!RO&r#WO&s!TO(r$nO~Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!O!_O!r!aO!s!aO!t!aO!u!aO!v!aO!x!cO&r#WO&s$wO])aXe)aXm)aX!V)aX!{)aX%v)aX(r)aX)T)aX)V)aX)W)aX~O)X$vO~P:nOPmO]eOe!POs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!VfO!X!XO!Y!WO!i!YO!{!dO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO)TaO)V|O)W{O)Y!ZO)[XO)hcO)idO~Ob%QOm:zO!|%RO(r$xO~P<lO)T%SO~Ob!]Om$_O|#RO#a#VO#b#TO%e#UO%l#SO&m!RO&r#WO&s!TO(r:}O~P<lOPmO]$eOb%QOm:zO!V$fO!W%_O!X!XO!Y!WO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)T$kO)W%]O)Y!ZO)[XO)hcO)idO)j%[O~O]%hOe!POm%bO!V%kO!{!dO%v$mO(r;OO)T%dO)V%iO)W%iO~O(v%mO~O)X#jO~O(r%nO](tX!V(tX!X(tX!Y(tX![(tX!^(tX!_(tX!a(tX!b(tX!c(tX!e(tX!f(tX!h(tX(u(tX(w(tX(x(tX)T(tX)U(tX)V(tX)W(tX)X(tX)Y(tX!g(tX)j(tX[(tX!W(tX(v(tX!U(tXQ(tX!d(tX~OP%oO(pQO~PCQO]%hOe!POs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!V%kO!r!aO!s!aO!t!aO!u!aO!v!aO!x!cO!{!dO%o!|O%v!}O)T;`O)V|O)W|O~Om%rO!o%wO(r$xO~PE_O!TxO#v!eO(v%yO)j%|O])eX!V)eX~O]%hOe!POm%rO!V%kO!{!dO%v!}O(r$xO)T;`O)V|O)W|O~O!TxO#v!eO)X&PO)j&QO~O!U&TO~P!QO]&YO!TxO!V&WO)T&VO)V&ZO)W&ZO~Oq&UO~PHrO]&cO!V&bO~OPmO]eOe!PO!VfO!X!XO!Y!WO!i!YO!{!dO#V#QO%_#ZO%`#[O%a#YO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO)TaO)V|O)W{O)Y!ZO)[XO)hcO)idO~Ob%QOm:zO%v$mO(r$xO~PIgO]%hOe!POm;[O!V%kO!{!dO%v$mO(r$xO)T;`O)V|O)W|O~Oq&fO](tX])eX!V(tX!V)eX!X(tX!Y(tX![(tX!^(tX!_(tX!a(tX!b(tX!c(tX!e(tX!f(tX!h(tX(u(tX(w(tX(x(tX)T(tX)U(tX)V(tX)W(tX)X(tX)Y(tX[(tX[)eX!U(tX~O!g$YO)j$YO~PL]O!g(tX)j(tX~PL]O](tX!V(tX!X(tX!Y(tX![(tX!^(tX!_(tX!a(tX!b(tX!c(tX!e(tX!f(tX!h(tX(u(tX(w(tX(x(tX)T(tX)U(tX)V(tX)W(tX)X(tX)Y(tX!g(tX)j(tX[(tX!U(tX~O])eX!V)eX[)eX~PNkOb&hO&m!RO]&lXe&lXm&lXs&lXt&lXu&lXv&lXw&lXx&lXy&lXz&lX!O&lX!V&lX!r&lX!s&lX!t&lX!u&lX!v&lX!x&lX!{&lX%v&lX&r&lX&s&lX(r&lX)T&lX)V&lX)W&lX)X&lX[&lX!T&lX!X&lX!Y&lX![&lX!^&lX!_&lX!a&lX!b&lX!c&lX!e&lX!f&lX!h&lX(u&lX(w&lX(x&lX)U&lX)Y&lX!g&lX)j&lX!W&lXQ&lX!d&lX(v&lX!U&lX#v&lX~Oq&fOm(}X[(}XQ(}X!d(}X!h(}X)Y(}X)j(}X~P.ZO!g$YO)j$YO](tX!V(tX!X(tX!Y(tX![(tX!^(tX!_(tX!a(tX!b(tX!c(tX!e(tX!f(tX!h(tX(u(tX(w(tX(x(tX)T(tX)U(tX)V(tX)W(tX)X(tX)Y(tX[(tX!W(tX(v(tX!U(tXQ(tX!d(tX~OPmO]$eOb%QOm:zO!V$fO!X!XO!Y!WO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)T$kO)W$kO)Y!ZO)[XO)hcO)idO~O](}Xe(}Xm(}Xs(}Xt(}Xu(}Xv(}Xw(}Xx(}Xy(}Xz(}X!O(}X!V(}X!r(}X!s(}X!t(}X!u(}X!v(}X!x(}X!{(}X%v(}X&r(}X&s(}X(r(}X)T(}X)V(}X)W(}X)X(}X[(}XQ(}X!d(}X!h(}X)Y(}X)j(}X~O]#}O~P!*qO]&lO~O])bXb)bXe)bXm)bXs)bXt)bXu)bXv)bXw)bXx)bXy)bXz)bX|)bX!O)bX!V)bX!o)bX!r)bX!s)bX!t)bX!u)bX!v)bX!x)bX!{)bX#a)bX#b)bX%e)bX%l)bX%o)bX%v)bX&m)bX&r)bX&s)bX(r)bX)T)bX)V)bX)W)bX~O(pQO~P!-ZO%U&nO~P!-ZO]&oO~O]#}O~O!TxO~O$W&wO(r%nO(v&vO~O]&xOx&zO~O]&xO~OPmO]$eOb%QOm:zO!TxO!V$fO!X!XO!Y!WO!i!YO#V#QO#p!xO#v!eO$Y!vO$Z!wO$`!iO$e!jO$g!kO$h!lO$k!mO$m!nO$o!oO$q!pO$s!qO$u!rO$w!sO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r:mO)QYO)T$kO)W$kO)XiO)Y!ZO)[XO)hcO)idO~O]'PO~O!T$WO)X'RO~P!(zO)X'TO~O)X'UO~O(r'VO~O)X'YO~P!(zOm;^O%U'^O%e'^O(r;PO~Ob!TOm$oOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO#a#VO#b#TO%e#UO%l#SO&m!RO&r#WO&s!TO(r$nO~O(v'bO~O)X'dO~P!(zO!TxO(r%nO)j'fO~O(r%nO~O]'iO~O]'jOe%nXm%nX!V%nX!{%nX%v%nX(r%nX)T%nX)V%nX)W%nX~O]'nO!V'oO!X'lO!g'lO%Z'lO%['lO%]'lO%^'lO%_'pO%`'pO%a'lO(x'mO)j'lO)x'qO~P8zO]%hOb!TOe!POs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO!O!_O!V%kO!r!aO!s!aO!t!aO!u!aO!v!aO!x!cO!{!dO#a#VO#b#TO%e#UO%l#SO&m!RO&r#WO&s!TO)T;`O)V|O)W|O~Om;_Oq&UO%v$mO(r;QO~P!8jO(r%nO(v'vO)X'wO~O]&cO!T'yO~Om$oO!O!_O!T(QO!l(VO(r$nO(v(PO)QYO~Om$oO|(^O!T(ZO#b(^O(r$nO~Ob!TOm$oO|#RO#a#VO#b#TO%e#UO%l#SO&m!RO&r#WO&s!TO(r$nO~O](`O~OPmOb%QOm:zO!V$fO!X!XO!Y!WO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)T$kO)W$kO)[XO)hcO)idO~O](bO)Y(cO~P!=UO]#}O~P!<[OPmO]$eOb%QOm:zO!V(iO!X!XO!Y!WO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)T$kO)W$kO)Y!ZO)[XO)hcO)idO~OY(jO(pQO(r%nO~O'f(mO~OS(qOT(nO*W(pO~O]#}O(n(tO~Q'nXX#dO(o(vO(q#dO~Oe)QOm({O&r#WO(r(zO~O!Y'Sa!['Sa!^'Sa!_'Sa!a'Sa!b'Sa!c'Sa!e'Sa!f'Sa!h'Sa(u'Sa)T'Sa)U'Sa)V'Sa)W'Sa)X'Sa)Y'Sa!g'Sa)j'Sa['Sa!W'Sa(v'Sa!U'SaQ'Sa!d'Sa~OPmOb%QOm:zO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)[XO)hcO)idO]'Sa!V'Sa!X'Sa(w'Sa(x'Sa~P!B_O!T$WO[(sP~P!(zO]oX]%WXeoXmnXqoXq%WXsoXtoXuoXvoXwoXxoXyoXzoX!OoX!ToX!VoX!V%WX!X%WX!Y%WX![%WX!^%WX!_%WX!a%WX!b%WX!c%WX!e%WX!f%WX!gnX!h%WX!roX!soX!toX!uoX!voX!xoX!{oX%voX&roX&soX(roX(u%WX(w%WX(x%WX)ToX)T%WX)U%WX)VoX)V%WX)WoX)W%WX)X%WX)Y%WX)jnX[%WX~O)XoX[oX!U%WX~P!E{O])dO!V)eO!X)bO!g)bO%Z)bO%[)bO%])bO%^)bO%_)fO%`)fO%a)bO(x)cO)j)bO)x)gO~P8zOPmO]$eOb%QOm:zO!X!XO!Y!WO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)T$kO)W$kO)Y!ZO)[XO)hcO)idO~O!V)lO~P!JwOe)oO%Y)pO(y$OO~O!T$WO!V)rO(w)sO!U)rP~P!JwO!T$WO~P!(zO)Z)zO~Om){O]!QX!h!QX)P!QX)Y!QX~O])}O!h*OO)P)RX)Y)RX~O)P*RO)Y*SO~Oe$RO%Y*TO'[$TO'_$UO(y$OO~Om*UO~Om*UO[(}X~P.ZOm*UO!g$YO)j$YO~O)X*VO~P:nOPmO]$eOb!]Om$_Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO!V$fO!X!XO!Y!WO!i!YO#V#QO#a#VO#b#TO%_#ZO%`#[O%a#YO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r:}O)T$kO)W$kO)Y!ZO)[XO)hcO)idO~Oq&fO~P!&zOq&fO!W(tX(v(tXQ(tX!d(tX~PNkO]'nO!V'oO!X'lO!g'lO%Z'lO%['lO%]'lO%^'lO%_'pO%`'pO%a'lO(x'mO)j'lO)x'qO~O]jXejXmhXqjXsjXtjXujXvjXwjXxjXyjXzjX!OjX!VjX!rjX!sjX!tjX!ujX!vjX!xjX!{jX%vjX&rjX&sjX(rjX)TjX)VjX)WjX!TjX!hjX)YjX)jjX[jX~O!ljX(vjX)XjX!XjX!YjX![jX!^jX!_jX!ajX!bjX!cjX!ejX!fjX(ujX(wjX(xjX)UjX!gjX!WjXQjX!djX!UjX#vjX#TjX#VjX#pjXbjX|jX!ojX#ajX#bjX#ijX#tjX${jX%cjX%ejX%kjX%ljX%ojX&mjX)QjX~P#%yO(y*ZO~Om*[O~O](}Xe(}Xs(}Xt(}Xu(}Xv(}Xw(}Xx(}Xy(}Xz(}X!O(}X!V(}X!r(}X!s(}X!t(}X!u(}X!v(}X!x(}X!{(}X%v(}X&r(}X&s(}X(r(}X)T(}X)V(}X)W(}X)X(}X!T(}X!X(}X!Y(}X![(}X!^(}X!_(}X!a(}X!b(}X!c(}X!e(}X!f(}X!h(}X(u(}X(w(}X(x(}X)U(}X)Y(}X!g(}X)j(}X[(}X!W(}XQ(}X!d(}X(v(}X!U(}X#v(}X~Om*[O~P#+ROs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!O!_O!r!aO!s!aO!t!aO!u!aO!v!aO!x!cO])aae)aam)aa!V)aa!{)aa%v)aa(r)aa)T)aa)V)aa)W)aaQ)aa!d)aa!h)aa)Y)aa)j)aa[)aa!T)aa(v)aa)X)aa~O&r#WO&s$wO~P#.qOq&fOm(}X~P#+RO&r)aa~P#.qO]ZXmhXqZXqjX!TjX!VZX!XZX!YZX![ZX!^ZX!_ZX!aZX!bZX!cZX!eZX!fZX!gZX!hZX(uZX(wZX(xZX)TZX)UZX)VZX)WZX)XZX)YZX)jZX[ZX~O!WZX(vZX!UZXQZX!dZX~P#1jO]#}O!V#lO!X#{O(w#kO(x#kO~O!Y&xa![&xa!^&xa!_&xa!a&xa!b&xa!c&xa!e&xa!f&xa!g&xa!h&xa(u&xa)T&xa)U&xa)V&xa)W&xa)X&xa)Y&xa)j&xa[&xa!W&xa(v&xa!U&xaQ&xa!d&xa~P#3zOm;hO!T$WO~Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O~PKkOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!|%RO~PKkO]&cO!V&bO[#Qa!T#Qa!h#Qa#v#Qa)X#Qa)j#QaQ#Qa!d#Qa(v#Qa~Oq&fO!T$WO~O[*cO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO[*cO~O[*eO]&cO!V&bO~O]&YOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!V&WO&r#WO&s$wO)T&VO)V&ZO)W&ZO~O[rXQrX!drX!hrX)YrX)XrX~P#9{O[*hO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h*iO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!W)kX~P#3zO!W*kO!h*lO~O!W*kO!h*lO~P!(zO!W*kO~Oq&fO!g$YO!h*mO)j$YO](tX!V(tX!W(tX!W*OX!X(tX!Y(tX![(tX!^(tX!_(tX!a(tX!b(tX!c(tX!e(tX!f(tX(u(tX(w(tX(x(tX)T(tX)U(tX)V(tX)W(tX)Y(tX~O!h(tX~P#=ZO!W*oO~Oe$RO%Y*TO(y:rO~Om;kO~Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!|%RO~PBUO]*vO!T*qO!V&bO!h*tO#v!eO)j*rO)X)qX~O!h*tO)X)qX~O)X*wO~Oq&fO])eX!T)eX!V)eX!h)eX#v)eX)X)eX)j)eX[)eXQ)eX!d)eX(v)eX~Oq&fO~OP%oO(pQO]%ha!V%ha!X%ha!Y%ha![%ha!^%ha!_%ha!a%ha!b%ha!c%ha!e%ha!f%ha!h%ha(r%ha(u%ha(w%ha(x%ha)T%ha)U%ha)V%ha)W%ha)X%ha)Y%ha!g%ha)j%ha[%ha!W%ha(v%ha!U%haQ%ha!d%ha~Oe$RO%Y$SO(y:oO~Om:wO~O!TxO#v!eO)j%|O~Om<[O&r#WO(r;gO~O$Z+TO%`+UO~O!TxO#v!eO)X+VO)j+WO~OPmO]$eOb%QOm:zO!V$fO!X!XO!Y!WO!i!YO#V#QO$Z+TO%_#ZO%`+YO%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)T$kO)W$kO)Y!ZO)[XO)hcO)idO~O!U+ZO~P!QOb!TOm$oOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO!O!_O!r!aO!s!aO!t!aO!u!aO!v!aO!x!cO#a+aO#b+bO#i+cO%e#UO%l#SO&m!RO&r#WO&s!TO(r$nO)QYO~OQ)lP!d)lP~P#GgO]&YOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!V&WO)T&VO)V&ZO)W&ZO~O[#kX!T#kX#v#kX)X#kX)j#kXQ#kX!d#kX!h#kX)Y#kX!x#kX(v#kX~P#IkOPmO]$eOb%QOm:zOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!V$fO!W+iO!X!XO!Y!WO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)T+jO)W$kO)Y!ZO)[XO)hcO)idO~O]&cO!V+kO~O]&YO!V&WO)QYO)T&VO)V&ZO)W&ZO)Y+nO[)dP~P8zO]&YO!V&WO)T&VO)V&ZO)W&ZO~O[#nX!T#nX#v#nX)X#nX)j#nXQ#nX!d#nX!h#nX)Y#nX!x#nX(v#nX~P#NeO!TxO])nX!V)nX~Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O#T+vO#p+wO(x+tO)V+rO)W+rO~O]#jX!T#jX!V#jX[#jX#v#jX)X#jX)j#jXQ#jX!d#jX!h#jX)Y#jX!x#jX(v#jX~P$ xO#V+yO~Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!l+zO#T+vO#V+yO#p+wO(x+tO)V+zO)W+zO])fP!T)fP!V)fP#v)fP(v)fP)j)fP[)fP!h)fP)X)fP~O!x)fPQ)fP!d)fP~P$#uOPmO]$eOb%QOm:zOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!V$fO!X!XO!Y!WO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)W$kO)Y!ZO)[XO)hcO)idO~O!W,QO)T,RO~P$%pO)QYO)Y+nO[)dP~P8zO]&cO!V&bO[&Za!T&Za!h&Za#v&Za)X&Za)j&ZaQ&Za!d&Za(v&Za~OPmO]$eOb!]Om:|Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO!V$fO!X!XO!Y!WO!i!YO#V#QO#a#VO#b#TO%_#ZO%`#[O%a#YO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r;RO)T$kO)W$kO)Y!ZO)[XO)hcO)idO~OQ(zP!d(zP~P$)YO]#}O!V#lO(w#kO(x#kO!X'Pa!Y'Pa!['Pa!^'Pa!_'Pa!a'Pa!b'Pa!c'Pa!e'Pa!f'Pa!h'Pa(u'Pa)T'Pa)U'Pa)V'Pa)W'Pa)X'Pa)Y'Pa!g'Pa)j'Pa['Pa!W'Pa(v'Pa!U'PaQ'Pa!d'Pa~O]#}O!V#lO!X#{O(w#kO(x#kO~P!B_O!TxO#t!fO)QYO~P8zO!TxO(r%nO)j,[O~O#x,aO~OQ)aX!d)aX!h)aX)Y)aX)j)aX[)aX!T)aX(v)aX)X)aX~P:nO(v,eO(w,cO)Q$UX)X$UX~O(r,fO~O)QYO)X,iO~OPmO]$eOb!]Om:{Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO!O!_O!V$fO!X!XO!Y!WO!i!YO!r!aO!s!aO!t!aO!u!aO!v!aO!x!cO#V#QO#a#VO#b#TO%_#ZO%`#[O%a#YO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO)QYO)T$kO)W$kO)XiO)Y!ZO)[XO)hcO)idO~O(r;SO~P$0kOPmO]$eOb%QOm:zO!TxO!V$fO!X!XO!Y!WO!i!YO#V#QO#v!eO$Y!vO$Z!wO$`!iO$e!jO$g!kO$h!lO$k!mO$m!nO$o!oO$q!pO$s!qO$u!rO$w!sO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r:mO)QYO)T$kO)W$kO)XiO)Y!ZO)[XO)hcO)idO~O$h,sO~OPmO]$eOb!]Om:{Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO!O!_O!V$fO!X!XO!Y!WO!i!YO!r!aO!s!aO!t!aO!u!aO!v!aO!x!cO#V#QO#a#VO#b#TO$}!uO%_#ZO%`#[O%a#YO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO)QYO)T$kO)W$kO)Y!ZO)[XO)hcO)idO~O${,yO(r:}O)X,wO~P$7UO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)X,{O)Y#|O~P#3zO)X,{O~O)X,|O~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X,}O)Y#|O~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X-OO)Y#|O~P#3zOq&fO)QYO)j-QO~O)X-RO~Om;^O(r;PO~O]-YO!{!dO&r#WO&s$wO(r-UO)T-VO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO(v-]O)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!TxO$`!iO$e!jO$g!kO$h!lO$k-bO$m!nO$o!oO$q!pO$s!qO$u!rO$w!sO$}!uO(r:nOe$Xa!o$Xa!{$Xa#i$Xa#p$Xa#t$Xa#v$Xa$R$Xa$T$Xa$Y$Xa$Z$Xa${$Xa%U$Xa%c$Xa%g$Xa%o$Xa%|$Xa(k$Xa)V$Xa!U$Xa$c$Xa~P$0kO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X-cO)Y#|O~P#3zOm-eO!TxO)j,[O~O)j-gO~O]&]a!X&]a!Y&]a![&]a!^&]a!_&]a!a&]a!b&]a!c&]a!e&]a!f&]a!h&]a(u&]a(w&]a(x&]a)U&]a)V&]a)W&]a)X&]a)Y&]a!g&]a)j&]a[&]a!W&]a!T&]a#v&]a(v&]a!U&]aQ&]a!d&]a~O)T-kO!V&]a~P$DbO[-kO~O!W-kO~O!V-lO)T&]a~P$DbO](}Xe(}Xs(}Xt(}Xu(}Xv(}Xw(}Xx(}Xy(}Xz(}X!O(}X!V(}X!r(}X!s(}X!t(}X!u(}X!v(}X!x(}X!{(}X%v(}X&r(}X&s(}X(r(}X)T(}X)V(}X)W(}X~Om;mO~P$GQO]&cO!V&bO)X-mO~Om;cO!o-pO#V+yO#i-uO#t!fO${,yO%c!zO%k-tO%o!|O%v!}O(r;TO)QYO~P!8jO!n-yO(r,fO~O)QYO)X-{O~OPmO]$eOb%QOm:zO!T.QO!V$fO!X!XO!Y!WO!i!YO#V.XO#a.WO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO(x.PO)T$kO)W$kO)X-}O)Y!ZO)[XO)hcO)idO~O!U.VO~P$JbO])^Xe)^Xs)^Xt)^Xu)^Xv)^Xw)^Xx)^Xy)^Xz)^X!O)^X!T)^X!V)^X!l)^X!r)^X!s)^X!t)^X!u)^X!v)^X!x)^X!{)^X%v)^X&r)^X&s)^X(r)^X(v)^X)T)^X)V)^X)W)^X)X)^X[)^X!h)^X)Y)^X!X)^X!Y)^X![)^X!^)^X!_)^X!a)^X!b)^X!c)^X!e)^X!f)^X(u)^X(w)^X(x)^X)U)^X!g)^X)j)^X!W)^XQ)^X!d)^X#T)^X#V)^X#p)^X#v)^Xb)^X|)^X!o)^X#a)^X#b)^X#i)^X#t)^X${)^X%c)^X%e)^X%k)^X%l)^X%o)^X&m)^X)Q)^X!U)^X~Om*[O~P$LlOm$oO!T(QO!l.^O(r$nO(v(PO)QYO~Oq&fOm)^X~P$LlOm$oO!n.cO!o.cO(r$nO)QYO~Om;dO!U.nO!n.pO!o.oO#i-uO${!tO$}!uO%g!{O%k-tO%o!|O%v!}O(r;VO)QYO~P!8jO!T(QO!l.^O(v(PO])OXe)OXm)OXs)OXt)OXu)OXv)OXw)OXx)OXy)OXz)OX!O)OX!V)OX!r)OX!s)OX!t)OX!u)OX!v)OX!x)OX!{)OX%v)OX&r)OX&s)OX(r)OX)T)OX)V)OX)W)OX~O)X)OX[)OX!X)OX!Y)OX![)OX!^)OX!_)OX!a)OX!b)OX!c)OX!e)OX!f)OX!h)OX(u)OX(w)OX(x)OX)U)OX)Y)OX!g)OX)j)OX!W)OXQ)OX!d)OX!U)OX#v)OX~P%%eO!T(QO~O!T(QO(v(PO~O(r%nO!U*QP~O!T(ZO(v.uO]&kae&kam&kas&kat&kau&kav&kaw&kax&kay&kaz&ka!O&ka!V&ka!r&ka!s&ka!t&ka!u&ka!v&ka!x&ka!{&ka%v&ka&r&ka&s&ka(r&ka)T&ka)V&ka)W&ka)X&ka[&ka!X&ka!Y&ka![&ka!^&ka!_&ka!a&ka!b&ka!c&ka!e&ka!f&ka!h&ka(u&ka(w&ka(x&ka)U&ka)Y&ka!g&ka)j&ka!W&kaQ&ka!d&ka!U&ka#v&ka~Om$oO!T(ZO(r$nO~O&r#WO&s$wO]&pae&pam&pas&pat&pau&pav&paw&pax&pay&paz&pa!O&pa!V&pa!r&pa!s&pa!t&pa!u&pa!v&pa!x&pa!{&pa%v&pa(r&pa)T&pa)V&pa)W&pa)X&pa[&pa!T&pa!X&pa!Y&pa![&pa!^&pa!_&pa!a&pa!b&pa!c&pa!e&pa!f&pa!h&pa(u&pa(w&pa(x&pa)U&pa)Y&pa!g&pa)j&pa!W&paQ&pa!d&pa(v&pa!U&pa#v&pa~O&s.zO~P!(zO!Y#qO![#rO!f#zO)T#mO!^'Ua!_'Ua!a'Ua!b'Ua!c'Ua!e'Ua!h'Ua(u'Ua)U'Ua)V'Ua)W'Ua)X'Ua)Y'Ua!g'Ua)j'Ua['Ua!W'Ua(v'Ua!U'UaQ'Ua!d'Ua~P#3zO!V'dX!X'dX!Y'dX!['dX!^'dX!_'dX!a'dX!b'dX!c'dX!e'dX!f'dX!h'dX(u'dX(w'dX(x'dX)T'dX)U'dX)V'dX)W'dX)Y'dX['dX~O].|O)X'dX!g'dX)j'dX!W'dX(v'dX!U'dXQ'dX!d'dX~P%2xO!Y#qO![#rO!f#zO)T#mO!^'Wa!_'Wa!a'Wa!b'Wa!c'Wa!e'Wa!h'Wa(u'Wa)U'Wa)V'Wa)W'Wa)X'Wa)Y'Wa!g'Wa)j'Wa['Wa!W'Wa(v'Wa!U'WaQ'Wa!d'Wa~P#3zO]#}O!T$WO!V.}O&r#WO&s$wO~O!X'Za!Y'Za!['Za!^'Za!_'Za!a'Za!b'Za!c'Za!e'Za!f'Za!h'Za(u'Za(w'Za(x'Za)T'Za)U'Za)V'Za)W'Za)X'Za)Y'Za!g'Za)j'Za['Za!W'Za(v'Za!U'ZaQ'Za!d'Za~P%6oO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!h'^a)X'^a!g'^a)j'^a['^a!W'^a(v'^a!U'^aQ'^a!d'^a~P#3zOPmO]$eOb%QOm:zO!V$fO!X!XO!Y!WO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)T$kO)W%]O)Y!ZO)[XO)hcO)idO)j%[O~O!W/QO~P%:oOS(qOT(nO]#}O*W(pO~O]/TO'f/UO*W/RO~OS/YOT(nO*W/XO~O]#}O~Q'na!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO(v/[O)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O)X#Zi[#Zi~P#3zO]dXmhXqdXqjX!VdX!XdX!YdX![dX!^dX!_dX!adX!bdX!cdX!edX!fdX!gdX!hdX(udX(wdX(xdX)TdX)UdX)VdX)WdX)XdX)YdX)jdX[dX!WdX(vdX!TdX#vdX!UdXQdX!ddX~Oe/^O%Y*TO(y/]O~Om/_O~Om/`O~Oq&fO]ci!Vci!Xci!Yci![ci!^ci!_ci!aci!bci!cci!eci!fci!gci!hci(uci(wci(xci)Tci)Uci)Vci)Wci)Xci)Yci)jci[ci!Wci(vci!UciQci!dci~O!W/bO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO![#rO)T#mO!Y&zi!^&zi!_&zi!a&zi!b&zi!c&zi!e&zi!f&zi!h&zi(u&zi)U&zi)V&zi)W&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y&zi![&zi!^&zi!_&zi!a&zi!b&zi!c&zi!e&zi!f&zi!h&zi(u&zi)T&zi)U&zi)V&zi)W&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO)T#mO)W#pO!h&zi(u&zi)U&zi)V&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO)T#mO)V#nO)W#pO!h&zi(u&zi)U&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y#qO![#rO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO)T#mO)W#pO!^&zi!h&zi(u&zi)U&zi)V&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y#qO![#rO!a#xO!b#yO!c#yO!e#yO!f#zO)T#mO)W#pO!^&zi!_&zi!h&zi(u&zi)U&zi)V&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y#qO![#rO!a#xO!b#yO!c#yO!e#yO!f#zO)T#mO!^&zi!_&zi!h&zi(u&zi)U&zi)V&zi)W&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y#qO![#rO!b#yO!c#yO!e#yO!f#zO)T#mO!^&zi!_&zi!a&zi!h&zi(u&zi)U&zi)V&zi)W&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y#qO![#rO!f#zO)T#mO!^&zi!_&zi!a&zi!b&zi!c&zi!e&zi!h&zi(u&zi)U&zi)V&zi)W&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y#qO![#rO)T#mO!^&zi!_&zi!a&zi!b&zi!c&zi!e&zi!f&zi!h&zi(u&zi)U&zi)V&zi)W&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO)T#mO)U#oO)V#nO)W#pO!h&zi(u&zi)X&zi)Y&zi!g&zi)j&zi[&zi!W&zi(v&zi!U&ziQ&zi!d&zi~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h/cO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O[(sX~P#3zO!h/cO[(sX~O[/eO~O]%Xaq%Xa!X%Xa!Y%Xa![%Xa!^%Xa!_%Xa!a%Xa!b%Xa!c%Xa!e%Xa!f%Xa!h%Xa(u%Xa(w%Xa(x%Xa)U%Xa)V%Xa)W%Xa)X%Xa)Y%Xa!g%Xa)j%Xa[%Xa!W%Xa!T%Xa#v%Xa(v%Xa!U%XaQ%Xa!d%Xa~O)T/fO!V%Xa~P&,aO[/fO~O!W/fO~O!V/gO)T%Xa~P&,aO!X'Zi!Y'Zi!['Zi!^'Zi!_'Zi!a'Zi!b'Zi!c'Zi!e'Zi!f'Zi!h'Zi(u'Zi(w'Zi(x'Zi)T'Zi)U'Zi)V'Zi)W'Zi)X'Zi)Y'Zi!g'Zi)j'Zi['Zi!W'Zi(v'Zi!U'ZiQ'Zi!d'Zi~P%6oO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!h'^i)X'^i!g'^i)j'^i['^i!W'^i(v'^i!U'^iQ'^i!d'^i~P#3zO!W/lO~P%:oO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h/nO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!U)rX~P#3zO(r/qO~O!V/sO(w)sO)j/uO~O!h/nO!U)rX~O!U/vO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO)T#mO)U#oO)V#nO)W#pO)Y#|O!h`i(u`i)X`i!g`i)j`i[`i!W`i(v`i!U`iQ`i!d`i~P#3zO!R/wO~Om){O]!Qa!h!Qa)P!Qa)Y!Qa~OP0PO]0OOm0PO!R0PO!T/|O!V/}O!X0PO!Y0PO![0PO!^0PO!_0PO!a0PO!b0PO!c0PO!e0PO!f0PO!g0PO!h0PO!i0PO(pQO(v0PO(w0PO(x0PO)T/yO)U/zO)V/zO)W/{O)X0PO)Y0PO)[XO~O[0SO~P&6yO!R$[O~O!h*OO)P)Ra)Y)Ra~O)P0WO~O])dO!V)eO!X)bO!g)bO%Z)bO%[)bO%])bO%^)bO%_)fO%`)fO%a)bO(x)cO)j)bO)x)gO~Oe)oO%Y*TO(y$OO~O)X0YO~O]oXeoXmnXqoXsoXtoXuoXvoXwoXxoXyoXzoX!OoX!VoX!roX!soX!toX!uoX!voX!xoX!{oX%voX&roX&soX(roX)ToX)VoX)WoX!ToX!hoX)YoX[oXQoX!doX~O!loX(voX)XoX!XoX!YoX![oX!^oX!_oX!aoX!boX!coX!eoX!foX(uoX(woX(xoX)UoX!goX)joX!WoX!UoX#voX#ToX#VoX#poXboX|oX!ooX#aoX#boX#ioX#toX${oX%coX%eoX%koX%loX%ooX&moX)QoX~P&:uOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!O!_O!r!aO!s!aO!t!aO!u!aO!v!aO!x!cO~O])aie)aim)ai!V)ai!{)ai%v)ai(r)ai)T)ai)V)ai)W)aiQ)ai!d)ai!h)ai)Y)ai)j)ai[)ai!T)ai&r)ai(v)ai)X)ai~P&?sO]&cO!V&bO[#Qi!T#Qi!h#Qi#v#Qi)X#Qi)j#QiQ#Qi!d#Qi(v#Qi~O[raQra!dra!hra)Yra)Xra~P#9{O[raQra!dra!hra)Yra)Xra~P#IkO]&cO!V+kO[raQra!dra!hra)Yra)Xra~O!h*iO!W)ka~O!h*mO!W*Oa~OPmOb!]Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O|#RO!O!_O!X!XO!Y!WO!i!YO!s!aO!t!aO!v!aO!x!cO#V#QO#a#VO#b#TO#v!eO$Y!vO$Z!wO$`!iO$e!jO$g!kO$h!lO$k!mO$m!nO$o!oO$q!pO$s!qO$u!rO$w!sO%_#ZO%`#[O%a#YO%e#UO%l#SO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO)QYO)XiO)Y!ZO)[XO)hcO)idO~O]eOe!POmTO!T*qO!U&TO!V0hO!opO!r!`O!u!bO!{!dO#i#OO#p!xO#t!fO$R!gO$T!hO${!tO$}!uO%U!yO%c!zO%g!{O%o!|O%v!}O%|#PO(rRO(w)sO)TaO)V|O)W{O~P&DuO!h*tO)X)qa~OPmO]$eOb!]Om:|O|#RO!T$WO!V$fO!X!XO!Y!WO!i!YO#V#QO#a#VO#b#TO%_#ZO%`#[O%a#YO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r;UO)QYO)T$kO)W$kO)Y0nO)[XO)hcO)idO[(sP[)dP~P&?sO!h*mO!W*OX~O]#}O!T$WO~O!h0sO!T)zX#v)zX)j)zX~O)X0uO~O)X0vO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X0xO)Y#|O~P#3zO)X0vO~P!?WO]1SOe!POm%bO!V1QO!{!dO%v$mO(r$xO)T0zO)Y0}O~O)V1OO)W1OO)j0{OQ#PX!d#PX!h#PX[#PX~P' dO!h1TOQ)lX!d)lX~OQ1VO!d1VO~O)Y1YO)j1XOQ#`X!d#`X!h#`X~P!<[O)Y1YO)j1XOQ#`X!d#`X!h#`X~P!;bOq&UO~O[#ka!T#ka#v#ka)X#ka)j#kaQ#ka!d#ka!h#ka)Y#ka!x#ka(v#ka~P#IkO]&cO!V+kO[#ka!T#ka#v#ka)X#ka)j#kaQ#ka!d#ka!h#ka)Y#ka!x#ka(v#ka~O!W1_O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!W1_O)T1aO~P$%pO!W1_O~P!(zO]#ja!T#ja!V#ja[#ja#v#ja)X#ja)j#jaQ#ja!d#ja!h#ja)Y#ja!x#ja(v#ja~P$ xO[1eO]&cO!V+kO~O!h1fO[)dX~O[1hO~O]&cO!V+kO[#na!T#na#v#na)X#na)j#naQ#na!d#na!h#na)Y#na!x#na(v#na~O]1lOs#SXt#SXu#SXv#SXw#SXx#SXy#SXz#SX!T#SX!V#SX#T#SX#p#SX(x#SX)V#SX)W#SX!l#SX!x#SX#V#SX#v#SX(v#SX)j#SX[#SX!h#SX)X#SXQ#SX!d#SX)Y#SX~O]1mO~O]1pOm$oO!V$fO#V#QO(r$nO)hcO)idO~Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!l+zO#T+vO#V+yO#p+wO(x+tO)V+zO)W+zO~O])fX!T)fX!V)fX!x)fX#v)fX(v)fX)j)fX[)fX!h)fX)X)fXQ)fX!d)fX~P'+}O!x!cO]#Ri!T#Ri!V#Ri#v#Ri(v#Ri)j#Ri[#Ri!h#Ri)X#RiQ#Ri!d#Ri~O!W1xO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!W1xO)T1zO~P$%pO!W1xO~P!(zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|OQ*RX!d*RX!h*RX~P#3zO)Y1{OQ({X!d({X!h({X~O!h1|OQ(zX!d(zX~OQ2OO!d2OO~O[2PO~O#t$lO)QYO~P8zOm-eO!TxO)j2TO~O[2UO~O#x,aOP#ui]#uib#uie#uim#uis#uit#uiu#uiv#uiw#uix#uiy#uiz#ui|#ui!O#ui!T#ui!V#ui!X#ui!Y#ui!i#ui!o#ui!r#ui!s#ui!t#ui!u#ui!v#ui!x#ui!{#ui#V#ui#a#ui#b#ui#i#ui#p#ui#t#ui#v#ui$R#ui$T#ui$Y#ui$Z#ui$`#ui$e#ui$g#ui$h#ui$k#ui$m#ui$o#ui$q#ui$s#ui$u#ui$w#ui${#ui$}#ui%U#ui%_#ui%`#ui%a#ui%c#ui%e#ui%g#ui%l#ui%o#ui%v#ui%|#ui&m#ui&r#ui&s#ui'Q#ui'R#ui'V#ui'Y#ui'a#ui'b#ui(k#ui(p#ui(r#ui)Q#ui)T#ui)V#ui)W#ui)X#ui)Y#ui)[#ui)h#ui)i#ui!U#ui$c#ui!n#ui%k#ui~O]&cO~O]&cO!TxO!V&bO#v!eO~O(v2ZO(w,cO)Q$Ua)X$Ua~O)QYO)X2]O~O[2^O~P,]O[2^O)X#jO~O[2^O~O$c2cOP$_i]$_ib$_ie$_im$_is$_it$_iu$_iv$_iw$_ix$_iy$_iz$_i|$_i!O$_i!T$_i!V$_i!X$_i!Y$_i!i$_i!o$_i!r$_i!s$_i!t$_i!u$_i!v$_i!x$_i!{$_i#V$_i#a$_i#b$_i#i$_i#p$_i#t$_i#v$_i$R$_i$T$_i$Y$_i$Z$_i$`$_i$e$_i$g$_i$h$_i$k$_i$m$_i$o$_i$q$_i$s$_i$u$_i$w$_i${$_i$}$_i%U$_i%_$_i%`$_i%a$_i%c$_i%e$_i%g$_i%l$_i%o$_i%v$_i%|$_i&m$_i&r$_i&s$_i'Q$_i'R$_i'V$_i'Y$_i'a$_i'b$_i(k$_i(p$_i(r$_i)Q$_i)T$_i)V$_i)W$_i)X$_i)Y$_i)[$_i)h$_i)i$_i!U$_i~O]1pO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)X2fO)Y#|O~P#3zOPmO]$eOb!]Om:{O|#RO!V$fO!X!XO!Y!WO!i!YO#V#QO#a#VO#b#TO%_#ZO%`#[O%a#YO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r:}O)T$kO)W$kO)X2iO)Y!ZO)[XO)hcO)idO~P&?sO)X2fO~O(r-UO~O)QYO)j2lO~O)X2nO~O]-YOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!{!dO!|%RO(r-UO)T-VO~O)T2sO~O]&cO!V2uO!h2vO)X)uX~O]-YO!{!dO(r-UO)T-VO~O)X2yO~O!TxO$`!iO$e!jO$g!kO$h!lO$k-bO$m!nO$o!oO$q!pO$s!qO$u!rO$w!sO$}!uO(r:nOe$Xi!o$Xi!{$Xi#i$Xi#p$Xi#t$Xi#v$Xi$R$Xi$T$Xi$Y$Xi$Z$Xi${$Xi%U$Xi%c$Xi%g$Xi%o$Xi%|$Xi(k$Xi)V$Xi!U$Xi$c$Xi~P$0kOm:{O(r:nO~P0zO]2}O~O)X2SO~O!u3PO(r%nO~O[3SO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h3TO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO[3UO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO]&cO!V+kO!T%ui#v%ui)X%ui)j%ui~O!W3VO~Om:yO)X(}X~P$GQOb!TOm$oO|3]O#a#VO#b3[O#t!fO%e#UO%l3^O&m!RO&r#WO&s!TO(r$nO)QYO~P&?sOm;cO!o-pO#i-uO#t!fO${,yO%c!zO%k-tO%o!|O%v!}O(r;TO)QYO~P!8jO]&cO!V&bO)X3`O~O)X3aO~O)QYO)X3aO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)X3bO)Y#|O~P#3zO)X3bO~O)X3eO~O!U3gO~P$JbOm$oO(r$nO~O]3iO!T'yO~P'+iO!T(QO!l3lO(v(PO])Oae)Oam)Oas)Oat)Oau)Oav)Oaw)Oax)Oay)Oaz)Oa!O)Oa!V)Oa!r)Oa!s)Oa!t)Oa!u)Oa!v)Oa!x)Oa!{)Oa%v)Oa&r)Oa&s)Oa(r)Oa)T)Oa)V)Oa)W)Oa)X)Oa[)Oa!X)Oa!Y)Oa![)Oa!^)Oa!_)Oa!a)Oa!b)Oa!c)Oa!e)Oa!f)Oa!h)Oa(u)Oa(w)Oa(x)Oa)U)Oa)Y)Oa!g)Oa)j)Oa!W)OaQ)Oa!d)Oa!U)Oa#v)Oa~Om$oO!n.cO!o.cO(r$nO~O!h3pO)Y3rO!T)_X~O!o3tO)QYO~P8zO)X3uO~PGVO]3zOm({O!T$WO!{!dO%v$mO&r#WO(r(zO(v4OO)T3wO)V3{O)W3{O~O)X4PO)j4RO~P(&eOm;dO!U4TO!n.pO!o.oO#i-uO${!tO$}!uO%g!{O%k-tO%o!|O%v!}O(r;VO)QYO~P!8jOm;dO%v!}O(r;VO~P!8jO(v4UO~Om$oO!T(QO(r$nO(v(PO)QYO~O!l3lO~P((sO)j4WO!U&oX!h&oX~O!h4XO!U*QX~O!U4ZO~Ob4]Om$oO&m!RO(r$nO~O!T(ZO]&kie&kim&kis&kit&kiu&kiv&kiw&kix&kiy&kiz&ki!O&ki!V&ki!r&ki!s&ki!t&ki!u&ki!v&ki!x&ki!{&ki%v&ki&r&ki&s&ki(r&ki)T&ki)V&ki)W&ki)X&ki[&ki!X&ki!Y&ki![&ki!^&ki!_&ki!a&ki!b&ki!c&ki!e&ki!f&ki!h&ki(u&ki(w&ki(x&ki)U&ki)Y&ki!g&ki)j&ki!W&kiQ&ki!d&ki!U&ki#v&ki~O(v&ki~P(*TO(v.uO~P(*TO[4`O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO[4`O~O[4aO~O]#}O!T$WO!V'Zi!X'Zi!Y'Zi!['Zi!^'Zi!_'Zi!a'Zi!b'Zi!c'Zi!e'Zi!f'Zi!h'Zi(u'Zi(w'Zi(x'Zi)T'Zi)U'Zi)V'Zi)W'Zi)X'Zi)Y'Zi!g'Zi)j'Zi['Zi!W'Zi(v'Zi!U'ZiQ'Zi!d'Zi~OPmOb%QOm:zO!X!XO!Y!WO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)Y!ZO)[XO)hcO)idO]#]aq#]a!T#]a!V#]a)T#]a)V#]a)W#]a~O(r%nO)Y4fO[*YP~O*W4eO~O'f4hO*W4eO~O*W4iO~OmnXqoXq&wX~Oe4kO%Y*TO(y/]O~Oe4kO%Y*TO(y4lO~O!h/cO[(sa~O!W4pO~O]&cO!V+kO!T%uq#v%uq)X%uq)j%uq~O]#}O!T$WO!X'Zq!Y'Zq!['Zq!^'Zq!_'Zq!a'Zq!b'Zq!c'Zq!e'Zq!f'Zq!h'Zq(u'Zq(w'Zq(x'Zq)T'Zq)U'Zq)V'Zq)W'Zq)X'Zq)Y'Zq!g'Zq)j'Zq['Zq!W'Zq(v'Zq!U'ZqQ'Zq!d'Zq~O!V'Zq~P(5bO!V.}O&r#WO&s$wO~P(5bO!T$WO!V)rO(w)sO!U(UX!h(UX~P!JwO!h/nO!U)ra~O!W4xO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h*iO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!U4|O~P&6yO!W4|O~P&6yO[4|O~P&6yO[5RO~P&6yO]5SO!h'ua)P'ua)Y'ua~O!h*OO)P)Ri)Y)Ri~O]&cO!V&bO[#Qq!T#Qq!h#Qq#v#Qq)X#Qq)j#QqQ#Qq!d#Qq(v#Qq~O[riQri!dri!hri)Yri)Xri~P#IkO]&cO!V+kO[riQri!dri!hri)Yri)Xri~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!h'Tq)X'Tq!g'Tq)j'Tq['Tq!W'Tq(v'Tq!U'TqQ'Tq!d'Tq~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!W'|a!h'|a~P#3zO!W5XO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h5YO(u#gO)T#mO)U#oO)V#nO)W#pO)X#jO)Y#|O!U)rX~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!h#{i)X#{i~P#3zO]*vO!T$WO!V&bO)j*rO!h(Va)X(Va~O!h1fO[)dX]'dX~P%2xO)Y5[O!T%qa!h%qa#v%qa)j%qa~O!h0sO!T)za#v)za)j)za~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X5_O)Y#|O~P#3zO]1SOe!POm;[O!V1QO!{!dO%v$mO(r$xO)T;xO)V5aO)W5aO~OQ#Pa!d#Pa!h#Pa[#Pa~P(DjO]1SOe!POs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!V1QO!{!dO!|%RO%v$mO(r$xOQ#kX!d#kX!h#kX[#kX~Om%bO)T0zO)V;yO)W;yO~P(ElO]&cOQ#Pa!d#Pa!h#Pa[#Pa~O!V&bO)j5eO~P(GZO(r%nOQ#dX!d#dX!h#dX[#dX~O)V;yO)W;yOQ#nX!d#nX!h#nX[#nX~P' dO!V+kO~P(GZO]1SOb!TOe!POm;]O|#RO!V1QO!{!dO#a#VO#b#TO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO(r;QO)QYO)T;xO)V5aO)W5aO)Y+nO[)dP~P&?sO!h1TOQ)la!d)la~Oq&fO)j5jOQ#`am(}X!d#`a!h#`a)Y(}X~P$GQO(r-UOQ#ga!d#ga!h#ga~Oq&fO)j5jOQ#`a])^Xe)^Xm)^Xs)^Xt)^Xu)^Xv)^Xw)^Xx)^Xy)^Xz)^X!O)^X!T)^X!V)^X!d#`a!h#`a!l)^X!r)^X!s)^X!t)^X!u)^X!v)^X!x)^X!{)^X%v)^X&r)^X&s)^X(r)^X(v)^X)T)^X)V)^X)W)^X)Y)^X~O#a5mO#b5mO~O]&cO!V+kO[#ki!T#ki#v#ki)X#ki)j#kiQ#ki!d#ki!h#ki)Y#ki!x#ki(v#ki~O!W5oO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!W5oO~P!(zO!W5oO)T5qO~P$%pO]#ji!T#ji!V#ji[#ji#v#ji)X#ji)j#jiQ#ji!d#ji!h#ji)Y#ji!x#ji(v#ji~P$ xO)QYO)Y5sO~P8zO!h1fO[)da~O&r#WO&s$wO!T#qa!x#qa#v#qa(v#qa)j#qa[#qa!h#qa)X#qaQ#qa!d#qa)Y#qa~P#NeO[5xO~P!(zO[)oP~P!4xO)U6OO)V5|O]#Ua!T#Ua!V#Ua)T#Ua)W#Uas#Uat#Uau#Uav#Uaw#Uax#Uay#Uaz#Ua!l#Ua!x#Ua#T#Ua#V#Ua#p#Ua#v#Ua(v#Ua(x#Ua)j#Uab#Uae#Uam#Ua|#Ua!O#Ua!o#Ua!r#Ua!s#Ua!t#Ua!u#Ua!v#Ua!{#Ua#a#Ua#b#Ua#i#Ua#t#Ua${#Ua%c#Ua%e#Ua%k#Ua%l#Ua%o#Ua%v#Ua&m#Ua&r#Ua&s#Ua(r#Ua)Q#Ua)X#Ua[#Ua!h#UaQ#Ua!d#Ua~O!x!cO]#Rq!T#Rq!V#Rq#v#Rq(v#Rq)j#Rq[#Rq!h#Rq)X#RqQ#Rq!d#Rq~O!W6TO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!W6TO~P!(zO!h1|OQ(za!d(za~O)X6YO~Om-eO!TxO)j6ZO~O]*vO!T$WO!V&bO!h*tO)X)qX~O)j6_O~P)+cO[6aO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO[6aO~O$c6cOP$_q]$_qb$_qe$_qm$_qs$_qt$_qu$_qv$_qw$_qx$_qy$_qz$_q|$_q!O$_q!T$_q!V$_q!X$_q!Y$_q!i$_q!o$_q!r$_q!s$_q!t$_q!u$_q!v$_q!x$_q!{$_q#V$_q#a$_q#b$_q#i$_q#p$_q#t$_q#v$_q$R$_q$T$_q$Y$_q$Z$_q$`$_q$e$_q$g$_q$h$_q$k$_q$m$_q$o$_q$q$_q$s$_q$u$_q$w$_q${$_q$}$_q%U$_q%_$_q%`$_q%a$_q%c$_q%e$_q%g$_q%l$_q%o$_q%v$_q%|$_q&m$_q&r$_q&s$_q'Q$_q'R$_q'V$_q'Y$_q'a$_q'b$_q(k$_q(p$_q(r$_q)Q$_q)T$_q)V$_q)W$_q)X$_q)Y$_q)[$_q)h$_q)i$_q!U$_q~O)X6dO~OPmO]$eOb!]Om:{O|#RO!V$fO!X!XO!Y!WO!i!YO#V#QO#a#VO#b#TO%_#ZO%`#[O%a#YO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r:}O)T$kO)W$kO)X6fO)Y!ZO)[XO)hcO)idO~P&?sO(v6hO)j*rO~P)+cO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X6fO)Y#|O~P#3zO[6jO~P!(zO)X6nO~O)X6oO~O]-YOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!{!dO(r-UO)T-VO~O]&cO!V2uO!h%Oa)X%Oa[%Oa~O!W6uO)T6vO~P$%pO!h2vO)X)ua~O[6yO]&cO!V2uO~O!TxO$`!iO$e!jO$g!kO$h!lO$k-bO$m!nO$o!oO$q!pO$s!qO$u!rO$w!sO$}!uO(r:nOe$Xq!o$Xq!{$Xq#i$Xq#p$Xq#t$Xq#v$Xq$R$Xq$T$Xq$Y$Xq$Z$Xq${$Xq%U$Xq%c$Xq%g$Xq%o$Xq%|$Xq(k$Xq)V$Xq!U$Xq$c$Xq~P$0kOPmO]$eOb!]Om:{O|#RO!V$fO!X!XO!Y!WO!i!YO#V#QO#a#VO#b#TO%_#ZO%`#[O%a#YO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r:}O)QYO)T$kO)W$kO)X6{O)Y!ZO)[XO)hcO)idO~P&?sO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X7OO)Y#|O~P#3zO)X7PO~OP7QO(pQO~Om*[O)X)^X~P$GQOq&fOm(}X)X)^X~P$GQO)X7SO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O)X&Sa~P#3zO!U7UO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO)X7VO~OPmO]$eOb!]Om:|O|#RO!V$fO!X!XO!Y!WO!i!YO#V#QO#a#VO#b#TO%_#ZO%`#[O%a#YO%e#UO%l#SO%v$mO&m!RO&r#WO&s!TO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r;UO)QYO)T$kO)W$kO)Y0nO)[XO)hcO)idO[)dP~P&?sO!h3pO)Y7ZO!T)_a~O!h3pO!T)_a~O)X7`O)j7bO~P(&eO)X7dO~PGVO]3zOm({Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!{!dO!|%RO%v$mO&r#WO(r(zO)T3wO)V3{O)W3{O~O)T7hO~O]&cO!T*qO!V7jO!h7kO#v!eO(v4OO~O)X7`O)j7mO~P)FwO]3zOm({O!{!dO%v$mO&r#WO(r(zO)T3wO)V3{O)W3{O~Oq&fO])cX!T)cX!V)cX!h)cX#v)cX(v)cX)X)cX)j)cX[)cX~O)X7`O~O!T(QO!l7sO(v(PO])Oie)Oim)Ois)Oit)Oiu)Oiv)Oiw)Oix)Oiy)Oiz)Oi!O)Oi!V)Oi!r)Oi!s)Oi!t)Oi!u)Oi!v)Oi!x)Oi!{)Oi%v)Oi&r)Oi&s)Oi(r)Oi)T)Oi)V)Oi)W)Oi)X)Oi[)Oi!X)Oi!Y)Oi![)Oi!^)Oi!_)Oi!a)Oi!b)Oi!c)Oi!e)Oi!f)Oi!h)Oi(u)Oi(w)Oi(x)Oi)U)Oi)Y)Oi!g)Oi)j)Oi!W)OiQ)Oi!d)Oi!U)Oi#v)Oi~O(r%nO!U(fX!h(fX~O!h4XO!U*Qa~Oq&fO]*Pae*Pam*Pas*Pat*Pau*Pav*Paw*Pax*Pay*Paz*Pa!O*Pa!T*Pa!V*Pa!r*Pa!s*Pa!t*Pa!u*Pa!v*Pa!x*Pa!{*Pa%v*Pa&r*Pa&s*Pa(r*Pa)T*Pa)V*Pa)W*Pa)X*Pa[*Pa!X*Pa!Y*Pa![*Pa!^*Pa!_*Pa!a*Pa!b*Pa!c*Pa!e*Pa!f*Pa!h*Pa(u*Pa(w*Pa(x*Pa)U*Pa)Y*Pa!g*Pa)j*Pa!W*PaQ*Pa!d*Pa(v*Pa!U*Pa#v*Pa~O!T(ZO]&kqe&kqm&kqs&kqt&kqu&kqv&kqw&kqx&kqy&kqz&kq!O&kq!V&kq!r&kq!s&kq!t&kq!u&kq!v&kq!x&kq!{&kq%v&kq&r&kq&s&kq(r&kq)T&kq)V&kq)W&kq)X&kq[&kq!X&kq!Y&kq![&kq!^&kq!_&kq!a&kq!b&kq!c&kq!e&kq!f&kq!h&kq(u&kq(w&kq(x&kq)U&kq)Y&kq!g&kq)j&kq!W&kqQ&kq!d&kq(v&kq!U&kq#v&kq~OPmOb%QOm:zO!T$WO!i!YO#V#QO%_#ZO%`#[O%a#YO%v$mO'Q!WO'R!WO'V#XO'Y![O'a![O'b![O(pQO(r$xO)[XO)hcO)idO~O]*Ui!V*Ui!X*Ui!Y*Ui![*Ui!^*Ui!_*Ui!a*Ui!b*Ui!c*Ui!e*Ui!f*Ui!h*Ui(u*Ui(w*Ui(x*Ui)T*Ui)U*Ui)V*Ui)W*Ui)X*Ui)Y*Ui!g*Ui)j*Ui[*Ui!W*Ui(v*Ui!U*UiQ*Ui!d*Ui~P*&WO[7xO~O!W7yO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!h'^q)X'^q!g'^q)j'^q['^q!W'^q(v'^q!U'^qQ'^q!d'^q~P#3zO!h7zO[*YX~O[7|O~O*W7}O~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!h_y)X_y!g_y)j_y[_y!W_y(v_y!U_yQ_y!d_y~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O[(ha!h(ha~P#3zO]#}O!T$WO!V'Zy!X'Zy!Y'Zy!['Zy!^'Zy!_'Zy!a'Zy!b'Zy!c'Zy!e'Zy!f'Zy!h'Zy(u'Zy(w'Zy(x'Zy)T'Zy)U'Zy)V'Zy)W'Zy)X'Zy)Y'Zy!g'Zy)j'Zy['Zy!W'Zy(v'Zy!U'ZyQ'Zy!d'Zy~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!h'^y)X'^y!g'^y)j'^y['^y!W'^y(v'^y!U'^yQ'^y!d'^y~P#3zO]&cO!V+kO!T%uy#v%uy)X%uy)j%uy~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!U(Ua!h(Ua~P#3zO!W4xO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!U#}i!h#}i~P#3zO!U8PO~P&6yO!W8PO~P&6yO[8PO~P&6yO[8RO~P&6yO]&cO!V&bO[#Qy!T#Qy!h#Qy#v#Qy)X#Qy)j#QyQ#Qy!d#Qy(v#Qy~O]&cO!V+kO[rqQrq!drq!hrq)Yrq)Xrq~O]&cOQ#Pi!d#Pi!h#Pi[#Pi~O!V+kO~P*9jOQ#nX!d#nX!h#nX[#nX~P(DjO!V&bO~P*9jOQ(OX](OXe'qXm'qXs(OXt(OXu(OXv(OXw(OXx(OXy(OXz(OX!V(OX!d(OX!h(OX!{'qX%v'qX(r'qX)T(OX)V(OX)W(OX[(OX~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|OQ#_i!d#_i!h#_i[#_i~P#3zO&r#WO&s$wOQ#fi!d#fi!h#fi~O(r-UO)Y1YO)j1XOQ#`X!d#`X!h#`X~O!W8WO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!W8WO~P!(zO!T#qi!x#qi#v#qi(v#qi)j#qi[#qi!h#qi)X#qiQ#qi!d#qi)Y#qi~O]&cO!V+kO~P*?fO]&YO!V&WO&r#WO&s$wO)T&VO)V&ZO)W&ZO~P*?fO[8YO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!h8ZO[)oX~O[8]O~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|OQ*TX!d*TX!h*TX~P#3zO)Y8`OQ*SX!d*SX!h*SX~O)X8bO~O[$bi!h#{a)X#{a~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X8eO)Y#|O~P#3zO[8gO~P!(zO[8gO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO[8gO~O]&cO!V&bO(v8mO~O)X8nO~O]&cO!V2uO!h%Oi)X%Oi[%Oi~O!W8qO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!W8qO)T8sO~P$%pO!W8qO~P!(zO]&cO!V2uO!h(Ya)X(Ya~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)X8tO)Y#|O~P#3zO)X2iO~P!(zO)X8tO~OP%oO[8uO(pQO~O[8uO~O)X8vO~P%%eO#T8yO(x.PO)X8wO~O!h3pO!T)_i~O)Y8}O!T'wa!h'wa~O)X9PO)j9RO~P)FwO)X9PO~O)X9PO)j9VO~P(&eOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O~P)GgO]&cO!V7jO!T!ya!h!ya#v!ya(v!ya)X!ya)j!ya[!ya~O!W9^O)T9_O~P$%pO!T$WO!h7kO(v4OO)X9PO)j9VO~O!T$WO~P#EfO[9bO]&cO!V7jO~O]&cO!V7jO!T&aa!h&aa#v&aa(v&aa)X&aa)j&aa[&aa~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O)X&ba~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X9PO)Y#|O~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!U&oi!h&oi~P#3zO!V.}O]']i!T']i!X']i!Y']i![']i!^']i!_']i!a']i!b']i!c']i!e']i!f']i!h']i(u']i(w']i(x']i)T']i)U']i)V']i)W']i)X']i)Y']i!g']i)j']i[']i!W']i(v']i!U']iQ']i!d']i~O(r%nO)Y9eO~O!h7zO[*Ya~O[9gO~P&6yO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O!U(Ua)X#Zi~P#3zO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|OQ#_q!d#_q!h#_q[#_q~P#3zO&r#WO&s$wOQ#fq!d#fq!h#fq~O)j5jOQ#`a!d#`a!h#`a~O]&cO!V+kO!T#qq!x#qq#v#qq(v#qq)j#qq[#qq!h#qq)X#qqQ#qq!d#qq)Y#qq~O!h8ZO[)oa~O)V5|O]&Vi!T&Vi!V&Vi)T&Vi)U&Vi)W&Vis&Vit&Viu&Viv&Viw&Vix&Viy&Viz&Vi!l&Vi!x&Vi#T&Vi#V&Vi#p&Vi#v&Vi(v&Vi(x&Vi)j&Vib&Vie&Vim&Vi|&Vi!O&Vi!o&Vi!r&Vi!s&Vi!t&Vi!u&Vi!v&Vi!{&Vi#a&Vi#b&Vi#i&Vi#t&Vi${&Vi%c&Vi%e&Vi%k&Vi%l&Vi%o&Vi%v&Vi&m&Vi&r&Vi&s&Vi(r&Vi)Q&Vi)X&Vi[&Vi!h&ViQ&Vi!d&Vi~O)X9jO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O[$bq!h#{i)X#{i~P#3zO[9lO~P!(zO[9lO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO[9lO~O]&cO!V&bO(v9oO~O[9pO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO[9pO~O]&cO!V2uO!h%Oq)X%Oq[%Oq~O!W9tO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!W9tO~P!(zO)X6fO~P!(zO)X9uO~O)X9vO~O(x.PO)X9vO~O!h3pO!T)_q~O)Y9xO!T'wi!h'wi~O!T$WO!h7kO(v4OO)X9yO)j9{O~O)X9yO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X9yO)Y#|O~P#3zO)X9yO)j:OO~P)FwO]&cO!V7jO!T!yi!h!yi#v!yi(v!yi)X!yi)j!yi[!yi~O!W:SO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!W:SO)T:UO~P$%pO!W:SO~P!(zO]&cO!V7jO!T(da!h(da(v(da)X(da)j(da~O[:WO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO!h#iO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO[:WO~O[:]O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO[:]O~O]&cO!V2uO!h%Oy)X%Oy[%Oy~O)X:^O~O)X:_O~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X:_O)Y#|O~P#3zO!T$WO!h7kO(v4OO)X:_O)j:bO~O]&cO!V7jO!T!yq!h!yq#v!yq(v!yq)X!yq)j!yq[!yq~O!W:dO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO!W:dO~P!(zO[:fO!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)Y#|O~P#3zO[:fO~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X:hO)Y#|O~P#3zO)X:hO~O]&cO!V7jO!T!yy!h!yy#v!yy(v!yy)X!yy)j!yy[!yy~O!Y#qO![#rO!^#uO!_#vO!a#xO!b#yO!c#yO!e#yO!f#zO(u#gO)T#mO)U#oO)V#nO)W#pO)X:lO)Y#|O~P#3zO)X:lO~O]ZXmhXqZXqjX!TjX!VZX!XZX!YZX![ZX!^ZX!_ZX!aZX!bZX!cZX!eZX!fZX!gZX!hZX(uZX(v$]X(wZX(xZX)TZX)UZX)VZX)WZX)XZX)YZX)jZX~O]%WXmnXqoXq%WX!ToX!V%WX!X%WX!Y%WX![%WX!^%WX!_%WX!a%WX!b%WX!c%WX!e%WX!f%WX!gnX!h%WX(u%WX(w%WX(x%WX)T%WX)U%WX)V%WX)W%WX)Y%WX)jnX[%WXQ%WX!d%WX~O)X%WX!W%WX(v%WX!U%WX~P+GrO]oX]%WXeoXmnXqoXq%WXsoXtoXuoXvoXwoXxoXyoXzoX!OoX!VoX!V%WX!roX!soX!toX!uoX!voX!xoX!{oX%voX&roX&soX(roX)ToX)VoX)WoX[oX[%WX!hoX)YoX~O)XoX)joX~P+JSO]%WXmnXqoXq%WX!V%WX!h%WXQ%WX!d%WX[%WX~O!T%WX#v%WX)X%WX)j%WX(v%WX~P+LmOQoXQ%WX!ToX!X%WX!Y%WX![%WX!^%WX!_%WX!a%WX!b%WX!c%WX!doX!d%WX!e%WX!f%WX!gnX!h%WX(u%WX(w%WX(x%WX)T%WX)U%WX)V%WX)W%WX)Y%WX)jnX~P+JSO]oX]%WXmnXqoXq%WXsoXtoXuoXvoXwoXxoXyoXzoX!OoX!V%WX!roX!soX!toX!uoX!voX!xoX!{oX%voX&roX&soX(roX)ToX)VoX)WoX~O!ToX(voX)XoX)joX~P, eOeoX!VoX)X%WX~P, eOmnXqoX)X%WX~Oe)oO%Y)pO(y:oO~Oe)oO%Y)pO(y:tO~Oe)oO%Y)pO(y:pO~Oe$RO%Y*TO'[$TO'_$UO(y:oO~Oe$RO%Y*TO'[$TO'_$UO(y:qO~Oe$RO%Y*TO'[$TO'_$UO(y:sO~O[jX]jXsjXtjXujXvjXwjXxjXyjXzjX!VjX&rjX&sjX)TjX)VjX)WjXejX!OjX!rjX!sjX!tjX!ujX!vjX!xjX!{jX%vjX(rjX~P#1jO]ZXmhXqZXqjX!VZX!hZX)XZX)jZX~O!TZX#vZX(vZX~P,'{OmhXqjX)QjX)XZX)jjX~O]ZX]jXejXmhXqZXqjXsjXtjXujXvjXwjXxjXyjXzjX!OjX!VZX!VjX!rjX!sjX!tjX!ujX!vjX!xjX!{jX%vjX&rjX&sjX(rjX)TjX)VjX)WjX[ZX[jX!hjX)YjX)jjX~O)XZX~P,)VO]ZX]jXmhXqZXqjXsjXtjXujXvjXwjXxjXyjXzjX!TjX!VZX!VjX!XZX!YZX![ZX!^ZX!_ZX!aZX!bZX!cZX!eZX!fZX!gZX!hZX!hjX&rjX&sjX(uZX(wZX(xZX)TZX)TjX)UZX)VZX)VjX)WZX)WjX)YZX)YjX)jZX~OQZXQjX!dZX!djX~P,+pO]jXejXsjXtjXujXvjXwjXxjXyjXzjX!OjX!VjX!rjX!sjX!tjX!ujX!vjX!xjX!{jX%vjX&rjX&sjX(rjX)TjX)VjX)WjX~P#1jO]ZX]jXejXmhXqZXqjXsjXtjXujXvjXwjXxjXyjXzjX!OjX!VZX!VjX!rjX!sjX!tjX!ujX!vjX!xjX!{jX%vjX&rjX&sjX(rjX)TjX)VjX)WjX~O)XjX~P,0rO[ZX[jXejX!OjX!rjX!sjX!tjX!ujX!vjX!xjX!{jX%vjX(rjX)jjX~P,+pO]ZX]jXmhXqZXqjXsjXtjXujXvjXwjXxjXyjXzjX!OjX!TjX!VZX!rjX!sjX!tjX!ujX!vjX!xjX!{jX%vjX&rjX&sjX(rjX(vjX)TjX)VjX)WjX)XjX)jjX~Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O~PBUOe$RO%Y*TO(y:oO~Oe$RO%Y*TO(y:pO~Oe$RO%Y*TO(y:vO~Oe$RO%Y*TO(y:uO~O]%hOe!POm%bOs!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O!V%kO!{!dO!|%RO%v$mO(r$xO)T;aO)V;bO)W;bO~O]%hOe!POm%bO!V%kO!{!dO%v$mO(r$xO)T;aO)V;bO)W;bO~Oe$RO%Y$SO(y:pO~Oe$RO%Y$SO(y:tO~Om:yO~Om:xO~O]dXmhXqjX!TdX~Oe)oO%Y*TO(y:oO~Oe)oO%Y*TO(y:pO~Oe)oO%Y*TO(y:qO~Oe)oO%Y*TO(y:rO~Oe)oO%Y*TO(y:sO~Oe)oO%Y*TO(y:uO~Oe)oO%Y*TO(y:vO~Os!^Ot!^Ou!^Ov!^Ow!^Ox!^Oy!^Oz!^O~P,9OO](}Xs(}Xt(}Xu(}Xv(}Xw(}Xx(}Xy(}Xz(}X!O(}X!r(}X!s(}X!t(}X!u(}X!v(}X!x(}X!{(}X%v(}X&r(}X&s(}X(r(}X)T(}X)V(}X)W(}X)j(}X~Om:xO!T(}X(v(}X)X(}X~P,<}O]&wXmnXqoX!T&wX~Oe4kO%Y*TO(y;tO~Om;[O)T;xO)V5aO)W5aO~P(ElOe!POm%bO!{!dO%v$mO(r$xO~O]1SO!V1QO)T0zO)V;yO)W;yOQ#nX!d#nX!h#nX[#nX~P,?yO)T;YO~Om;hO~Om;iO~Om;jO~Om;lO~Om;mO~Om;nO~Om;lO!T$WOQ(}X!d(}X!h(}X)Y(}X[(}X)j(}X~P$GQOm;jO!T$WO~P$GQOm;hO!g$YO)j$YO~Om;jO!g$YO)j$YO~Om;lO!g$YO)j$YO~Om;iO[(}X!h(}X)Y(}X)j(}X~P$GQOe/^O%Y*TO(y;tO~Om;uO~O)T<YO~OV'e'h'i'g(p)[!R(rST%Z!Y!['je%[!i'R!f]'f*Z'k(w!^!_'l'm'l~",
  goto: "%5]*ZPPPPPP*[P*_PP.TPP4i7j7j:tP:t>OP>i>{?aFXMX!&]!,sP!3m!4b!5VP!5qPPPPPPPP!6[P!7tP!9V!:oP!:uPPPPPP!:xP!:xPP!:xPPPPPPPPP!;U!>lP!>oPP!?]!@QPPPPP!@UP>l!AgPP>l!Cn!Eo!E}!Gd!ITP!I`P!Io!Io!MP#!`##v#'S#*^!Eo#*hPP!Eo#*o#*u#*h#*h#*xP#*|#+k#+k#+k#+k!ITP#,U#,g#.|P#/bP#0}P#1R#1Z#2O#2Z#4i#4q#4q#1RP#1RP#4x#5OP#5YPP#5u#6d#7U#5uP#7v#8SP#5uP#5uPP#5u#5uP#5uP#5uP#5uP#5uP#5uP#5uP#8V#5Y#8sP#9YP#9o#9o#9o#9o#9|#1RP#:d#?`#?}PPPPPPPP#@uP#ATP#ATP#Aa#Dn#9OPP#@}#EQP#Ee#Ep#Ev#Ev#@}#FlP#1R#1R#1R#1R#1RP!Io#GW#G_#G_#G_#Gc!Ly#Gm!Ly#Gq!E}!E}!E}#Gt#L^!E}>l>l>l$#V!@Q!@Q!@Q!@Q!@Q!@Q!6[!6[!6[$#jP$%V$%e!6[$%kPP!6[$'y$'|#@l$(P:t7j$+V$-Q$.q$0a7jPP7j$2T7jP7j7jP7jP$5Z7jP7jPP7j$5gPPPPPPPPP*[P$8o$8u$;^$=d$=j$>Q$>[$>g$>v$>|$@[$AZ$Ab$Ai$Ao$Aw$BR$BX$Bd$Bj$Bs$B{$CW$C^$Ch$Cn$Cx$DP$D`$Df$DlP$Dr$Dz$ER$Ea$F}$GT$GZ$Gb$GkPPPPPPPP$Gq$GuPPPPP$Nw$'y$Nz%$S%&[PP%&i%&lPPPPPPPPP%&x%'{%(R%(V%)|%+Z%+|%,T%.d%.jPPP%.t%/P%/S%/Y%0a%0d%0n%0x%0|%2Q%2s%2y#@uP%3d%3t%3w%4X%4e%4i%4o%4u$'y$'|$'|%4x%4{P%5V%5YR#cP'`mO[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fU%om%p7QQ&m!`Q(j#]d0P)}/|/}0O0R4}5O5P5S8QR7Q3Tb}Oaewx{!g&S*q&v$i[!W!X!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$e$f$k%]%m&Q&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0{1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fS%`f0h#d%jgnp|#O$g$|$}%S%d%h%i%w&s't'u(Q*Y*`*b*t+],l,v-_-p-w.f.m.o0]0y0z1O1S2_2j5a6g;W;X;Y;`;a;b;o;p;q;r;v;w;x;y<W<X<YS%qm!YS&u!h#PQ']!tQ'g!yQ'h!zQ(j#`Q(k#]Q(l#^Q*x%kQ,W&lQ,]&nQ-S'^Q-d'fQ-k'qS.r(Z4XQ/f)gQ0e*mQ2Q,[Q2X,cQ3O-eQ4b.|Q4f/TQ5f0}Q6[2TQ6}3PQ8a6ZQ9e7zR;Z1Q$|#hS!]$y%Q%T%Z&j&k'Q'X'Z'a'c(a(e(h(w(x)R)S)T)U)V)W)X)Y)Z)[)])^)_)k)q)x+X+g,O,S,j,u-h-i-|.y/p0`0b0g0i0w1`1y2a2h3R3c3d4c4d4j4m4s4u4y4z5d5p5w6U6e6i6s6z7q7r7t8S8T8c8f8j8r9T9[9k9q9|:T:Y:`:iQ&p!dQ(g#ZQ(s#bQ)j$T[*s%e*W0k2`2g2{Q,^&oQ/O(fQ/S(kQ/Z(tS/i)i/PQ0r+QS4q/j/kR8O4r'a![O[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:f'a!VO[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fQ)O#kS+Q%y0sQ/r)sk3}.g3s3w3z3{7c7e7f7h7k9X9Y:RQ)Q#kk3|.g3s3w3z3{7c7e7f7h7k9X9Y:Rl)P#k.g3s3w3z3{7c7e7f7h7k9X9Y:RT+Q%y0s[UOwx!g&S*qW$`[e$e(b#l$p_!f!u!}#R#S#T#U#V#Z$S$T$l%U&U&Y&c&m'_'}(P(U(^(g)j)p+[+a+b+t+y,X,k,z-Q-o-t.W.X._.`.d.q.u1T1X1f1k1m2l3[3]3^3p3t5j5}6P7[8Z![%cg$g%d%i&s*Y*t+],l,v-_0z1O2_;W;X;Y;a;b;o;p;q;r;v;w;y<W<X<YY%snp%w-p.fl(|#k.g3s3w3z3{7c7e7f7h7k9X9Y:RS;e't-wU;f(Q.m.o&|;{af{|!W!X!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$f$k$|$}%S%]%h%m&Q&W&b&y&|'O'i'j'u'y(`(i)l)r*`*b*h*i*l*r+W+Y+h+j+k,P,R,n,q-g.P.Q.U.}/Q/[/c/l/n/s/u0]0h0y0{1Q1a1b1l1p1z2c2i2j2u4O4R4W4a5Y5a5e5q6_6c6f6g6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:f;`;xQ;|1Sd;}&x'P'b,w-]-^-a2f2z2}W<O&f*v1|3iQ<P#O[<Q!t'^'f,[2T6ZT<]%y0s[VOwx!g&S*qW$a[e$e(bQ$p.u!j$q_!f!u!}#V#Z$S$T$l%U&U&Y&c&m'_(g)j)p+[+a+t,X,k,z-Q-o.d1T1X1f1k1m2l3t5j8Z&^$zaf{!W!X!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$f$k%]%m&Q&W&b&y&|'O'i'j'y(`(i)l)r*h*i*l*r+W+Y+h+j+k,P,R,n,q-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z2c2i2u4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:f![%cg$g%d%i&s*Y*t+],l,v-_0z1O2_;W;X;Y;a;b;o;p;q;r;v;w;y<W<X<YY%snp%w-p.fQ'r#O|'|#R#S#T#U'}(P(U(^+b+y.W.X._.`.q3[3]3^3p5}6P7[l(|#k.g3s3w3z3{7c7e7f7h7k9X9Y:RS-n't-wQ3W-tU;s(Q.m.on;{|$|$}%S%h'u*`*b0]0y2j5a6g;`;x[<Q!t'^'f,[2T6ZW<R&f*v1|3id<S&x'P'b,w-]-^-a2f2z2}Q<Z1ST<]%y0s!Q!UO[ewx!g$e&S&f&x'P'b(b*q*v,w-]-^-a1|2f2z2}3i!v$t_!f!u!}#O#V#Z$S$T$l%U&U&Y&c&m'_'t(Q(g)j)p+[+t,X,k,z-Q-o-w.d.m.o1S1T1X1f1k1m2l3t5j8Z&^%Paf{!W!X!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$f$k%]%m&Q&W&b&y&|'O'i'j'y(`(i)l)r*h*i*l*r+W+Y+h+j+k,P,R,n,q-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z2c2i2u4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:f$Q%lgnp|#k$g$|$}%S%d%h%i%w%y&s'^'f'u*Y*`*b*t+],[,l,v-_-p.f.g0]0s0y0z1O2T2_2j3s3w3z3{5a6Z6g7c7e7f7h7k9X9Y:R;W;X;Y;`;a;b;o;p;q;r;v;w;x;y<W<X<YQ'[!tz(O#R#S#T#U'}(P(U(^+y.W.X._.`.q3[3]3^3p5}6P7[f-Z'`-T-V-Y2p2q2s2v6q6r8pQ1W+aQ1Z+bQ2k,yQ3X-tQ4[.uQ5l1YR8V5m!Q!UO[ewx!g$e&S&f&x'P'b(b*q*v,w-]-^-a1|2f2z2}3i!x$t_!f!u!}#O#V#Z$S$T$l%U&U&Y&c&m'_'t(Q(g)j)p+[+a+t,X,k,z-Q-o-w.d.m.o1S1T1X1f1k1m2l3t5j8Z&^%Paf{!W!X!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$f$k%]%m&Q&W&b&y&|'O'i'j'y(`(i)l)r*h*i*l*r+W+Y+h+j+k,P,R,n,q-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z2c2i2u4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:f$S%lgnp|!t#k$g$|$}%S%d%h%i%w%y&s'^'f'u*Y*`*b*t+],[,l,v-_-p.f.g0]0s0y0z1O2T2_2j3s3w3z3{5a6Z6g7c7e7f7h7k9X9Y:R;W;X;Y;`;a;b;o;p;q;r;v;w;x;y<W<X<Y|(O#R#S#T#U'}(P(U(^+b+y.W.X._.`.q3[3]3^3p5}6P7[Q3X-tR4[.u[WOwx!g&S*qW$b[e$e(b#l$p_!f!u!}#R#S#T#U#V#Z$S$T$l%U&U&Y&c&m'_'}(P(U(^(g)j)p+[+a+b+t+y,X,k,z-Q-o-t.W.X._.`.d.q.u1T1X1f1k1m2l3[3]3^3p3t5j5}6P7[8Z![%cg$g%d%i&s*Y*t+],l,v-_0z1O2_;W;X;Y;a;b;o;p;q;r;v;w;y<W<X<YY%snp%w-p.fl(|#k.g3s3w3z3{7c7e7f7h7k9X9Y:RS;e't-wU;f(Q.m.on;{|$|$}%S%h'u*`*b0]0y2j5a6g;`;xQ;|1SQ<P#O[<Q!t'^'f,[2T6Z&^<Taf{!W!X!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$f$k%]%m&Q&W&b&y&|'O'i'j'y(`(i)l)r*h*i*l*r+W+Y+h+j+k,P,R,n,q-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z2c2i2u4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fd<U&x'P'b,w-]-^-a2f2z2}W<V&f*v1|3iT<]%y0sp$PT$_$o%b%r({:z:{:|;[;];^;_;c;d<[o)m$V*U*[/_:w:x:y;h;i;j;k;l;m;n;up$QT$_$o%b%r({:z:{:|;[;];^;_;c;d<[o)n$V*U*[/_:w:x:y;h;i;j;k;l;m;n;u^&e}!O$i$j%`%j;Zd&i!U$t%P%l'[(O1W1Z3X4[V/a)O)P3}S%Ye$eQ,T&fQ.{(bQ2m-QQ5y1mQ6V1|Q6m2lR9h8Z#}!TO[_ewx!f!g!u!}#O#V#Z$S$T$e$l%U&S&U&Y&c&f&m&x'P'_'b't(Q(b(g)j)p*q*v+[+a+t,X,k,w,z-Q-]-^-a-o-t-w.d.m.o1S1T1X1f1k1m1|2f2l2z2}3i3t5j8Z#[^O[_`wx!f!g!}#O$S$d$l$s$u&S&U&Y&c&m&r&x'P'b't(Q)p*]*q*v+[,X,k,w,z-]-^-a-o-t-w.d.m.o1S1T1f2f2z2}3i3t_(U#R#S#T+b3[3]3^#}ZO[wx!g!k#R#S#T%m&S&U&Y&c&m&w&x&y&|'O'P'['b't'x'}(P(Q(U*q*v+[+b,X,h,k,q-P-]-^-a-o-t-w-z._.d.m.q1S1T1f2c2k2z2}3[3]3^3i6c6j8g9l9p:W:]:fQ$]YR0T*OR*Q$]e0P)}/|/}0O0R4}5O5P5S8Q'`!YO[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fe0P)}/|/}0O0R4}5O5P5S8QR5T0T^(T#R#S#T+b3[3]3^Y.]'}(R(U(V7TU3k.Z.^.qS7X3l4VR9c7s^(S#R#S#T+b3[3]3^[.['}(R(T(U(V7TW3j.Z.].^.qU7W3k3l4VS8z7X7sR:V9cT.k(Q.md]Owx!g&S't(Q*q-w.m!v^[_`!f!}#O$S$d$l$s$u&U&Y&c&m&r&x'P'b)p*]*v+[,X,k,w,z-]-^-a-o-t.d.o1S1T1f2f2z2}3i3tQ%tnT1u+}1v!jbOaenpwx{|!g#O$|$}%S%h%w&S't'u(Q*`*b*q-p-w.f.m.o0]0y1S2j5a6g;`;xf-W'`-T-V-Y2p2q2s2v6q6r8pj3x.g3s3w3z3{7c7e7f7h7k9X9Y:Rr;zg$g%d%i&s*Y*t,l,v-_2_;W;X;Y;o;q;vi<^+]0z1O;a;b;p;r;w;y<W<X<Y!O&^y%X&V&Y&Z'k)h*d*f+]+e+x/m0^0y0z1O1S1j5a5v;x;yz&az%O%W%e&d's*W*_,b-x0Z0[0k0|2`2g2{5V5b6l8iS'{#Q.Xn+l&X*g+f+m+p-j/h0_1R1^4t5W5`5u8XQ2W,a^2t-X2r2x6p6w8o9se7i3y7_7g7o7p9U9W9`:Q:cS+^&U1TY+n&Y&c*v1S3iR5s1f#w!POaegnpwx{|!g#O$g$|$}%S%d%h%i%w&S&s't'u(Q*Y*`*b*q*t+],l,v-_-p-w.f.m.o0]0y0z1O1S2_2j5a6g;W;X;Y;`;a;b;o;p;q;r;v;w;x;y<W<X<Y`oOwx!g&S't*q-w#U!Paeg{|#O$g$|$}%S%d%h%i&s'u*Y*`*b*t+],l,v-_0]0y0z1O1S2_2j5a6g;W;X;Y;`;a;b;o;p;q;r;v;w;x;y<W<X<YU%vnp-pQ*}%wS.e(Q.mT3v.f.oW+r&^+l+s1cV+z&a+{7iQ+x&`U+z&a+{7iQ-w'tT.S'y.U'`![O[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fX1r+y.X5}6P'W!VO[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/[/c/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fW1r+y.X5}6PR2e,s!WjO[wx!g!k%m&S&y&|'O'b*q,q-]-^-a2c2z6c6j8g9l9p:W:]:fY%Ve$e(b1p3iQ'S!nS(y#i5YQ,m&xQ,x'PS.O'y.UQ2b,nQ6k2iQ6|2}Q8h6fR9m8e'W![O[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/[/c/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fX1r+y.X5}6P'ayO[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k+y,P,R,n,q,w-]-^-a-g.P.Q.U.X.}/[/c/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W5Y5e5q5}6P6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fQ&`yS't#O-uR1[+cS+^&U1TR5n1[Q1P+]R5g1OR1P+]T+^&U1Tz&[%X&V&Y&Z'k)h*d*f+]+e/m0^0y0z1O1S1j5a5v;x;yQ&]yR1n+x!P&[y%X&V&Y&Z'k)h*d*f+]+e+x/m0^0y0z1O1S1j5a5v;x;yQ+u&^S+|&a7iS1d+l+sQ1t+{R5r1c!WkO[wx!g!k%m&S&y&|'O'b*q,q-]-^-a2c2z6c6j8g9l9p:W:]:fS%zo.eS&Oq-rQ&_yQ&q!eQ'e!yQ*p%eU*{%v%{3vS+P%x%}Q+q&]Q,Y&mS,Z&n'gQ,r&{S0X*W,bS0o*|*}Q0q+OQ1o+xS2S,]-fQ5U0ZQ5Z0pQ5{1nQ6Y2RQ6]2WQ7n3yQ9S7_R:P9U[uOwx!g&S*qQ,Y&mQ-v'tQ3Y-tR3_-wxlOwx!g!k%m&S&y'O*q,q2c6c6j8g9l9p:W:]:fU$h[&|-^S%zo.eS&Oq-rQ*p%eU*{%v%{3vS+P%x%}S0X*W,bS0o*|*}Q0q+OQ5U0ZQ5Z0pQ7n3yQ9S7_R:P9UT,_&q,`]uOwx!g&S*q[uOwx!g&S*qQ,Y&mQ,n&xQ,w'PW-`'b-]-a2zQ-v'tQ3Y-tQ3_-wR6{2}[%fg$g,l,v-_2_R0l*t^$XV!U$a$z%P<R<SQ'S!nS)`#}*vS)v$W*qQ)y$YY*s%e*W0k2g2{Q/O(fS/i)i/PS0a*h4aS0j*r6_Q0r+QQ4Q.gQ4n/cS4q/j/kS4v/n5YQ4{/uQ6`2`U7a3s3y4RQ8O4rQ8k6hY9Q7_7b7c7l7mQ9r8mW9z9O9R9U9VQ:Z9oU:a9{9}:OR:j:bS)v$W*qT4v/n5YZ)t$W)u*q/n5YQ&w!hR'x#PS,g&v'vQ2[,eR6^2ZxlOwx!g!k%m&S&y'O*q,q2c6c6j8g9l9p:W:]:fV$h[&|-^!XkO[wx!g!k%m&S&y&|'O'b*q,q-]-^-a2c2z6c6j8g9l9p:W:]:f!WhO[wx!g!k%m&S&y&|'O'b*q,q-]-^-a2c2z6c6j8g9l9p:W:]:fR'W!q!WkO[wx!g!k%m&S&y&|'O'b*q,q-]-^-a2c2z6c6j8g9l9p:W:]:fR,n&xQ&y!iQ&{!jQ'O!lR,q&zR,o&xxlOwx!g!k%m&S&y'O*q,q2c6c6j8g9l9p:W:]:fX-`'b-]-a2z[uOwx!g&S*qQ,z'PQ-v'tS.k(Q.mR3_-w[uOwx!g&S*qQ,z'PW-`'b-]-a2zT.k(Q.mg-Z'`-T-V-Y2p2q2s2v6q6r8pylOwx!g!k%m&S&y'O*q,q2c6c6j8g9l9p:W:]:fb!OOaewx{!g&S*q&|$j[f!W!X!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$e$f$k%]%m&Q&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:f#d%jgnp|#O$g$|$}%S%d%h%i%w&s't'u(Q*Y*`*b*t+],l,v-_-p-w.f.m.o0]0y0z1O1S2_2j5a6g;W;X;Y;`;a;b;o;p;q;r;v;w;x;y<W<X<YQ']!tQ-S'^Q-d'fQ2Q,[Q6[2TR8a6Zj$RT$_%b%r:z:{:|;[;];^;_;c;di)o$V*U:w:x:y;h;i;j;k;l;m;nj$RT$_%b%r:z:{:|;[;];^;_;c;dh)o$V*U:w:x:y;h;i;j;k;l;m;nS/^({<[V4k/_/`;u[uOwx!g&S*qQ-v'tR3_-w[uOwx!g&S*qT.k(Q.m'`!YO[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fR7R3T[uOwx!g&S*qQ-v'tS.k(Q.mR3_-w[pOwx!g&S*qQ%wnS-p't-wT.f(Q.mS%{o.eS*|%v3vR0p*}Q+R%yR5]0sS%zo.eS&Oq-rU*{%v%{3vS+P%x%}S0o*|*}Q0q+OQ5Z0pQ7n3yQ9S7_R:P9U`qOwx!g&S(Q*q.mS%xn-pU%}p.f.oQ+O%wT-r't-wS'z#Q.XR.Y'{T.R'y.US.S'y.UQ8x7UR9w8yT5}1q8_R6P1q#d!Pgnp|#O$g$|$}%S%d%h%i%w&s't'u(Q*Y*`*b*t+],l,v-_-p-w.f.m.o0]0y0z1O1S2_2j5a6g;W;X;Y;`;a;b;o;p;q;r;v;w;x;y<W<X<Yb!QOaewx{!g&S*q&}![[f!W!X!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$e$f$k%]%m&Q&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:f#d!Pgnp|#O$g$|$}%S%d%h%i%w&s't'u(Q*Y*`*b*t+],l,v-_-p-w.f.m.o0]0y0z1O1S2_2j5a6g;W;X;Y;`;a;b;o;p;q;r;v;w;x;y<W<X<Yb!QOaewx{!g&S*q&|![[f!W!X!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#{#}$U$W$Y$e$f$k%]%m&Q&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W4a5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fk3|.g3s3w3z3{7c7e7f7h7k9X9Y:RQ4Q.gS7a3s3yU9Q7_7c7lS9z9O9UR:a9}#|!TO[_ewx!f!g!u!}#O#V#Z$S$T$e$l%U&S&U&Y&c&f&m&x'P'_'b't(Q(b(g)j)p*q*v+[+a+t,X,k,w,z-Q-]-^-a-o-t-w.d.m.o1S1T1X1f1k1m1|2f2l2z2}3i3t5j8ZR4].uQ(]#US.v([(^S4^.w.xR7w4_Q.s(ZR7u4X#|!TO[_ewx!f!g!u!}#O#V#Z$S$T$e$l%U&S&U&Y&c&f&m&x'P'_'b't(Q(b(g)j)p*q*v+[+a+t,X,k,w,z-Q-]-^-a-o-t-w.d.m.o1S1T1X1f1k1m1|2f2l2z2}3i3t5j8Zp$w`$d$s%X&r'`(_(f)i*d-T/k1j5k5v8Uq(}#k%y.g0s3s3w3z3{7c7e7f7h7k9X9Y:RR,U&fR6W1|'X!VO[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/[/c/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:f$q#rS%T%Z'Q'X'Z'a'c(a(e(h(w(x)R)S)U)V)W)X)Y)Z)[)])^)_)k)q)x+X+g,O,S,j,u-h-i-|.y/p0`0b0g0i0w1`1y2a2h3R3c3d4c4d4j4m4s4u4y4z5d5p5w6U6e6i6s6z7q7r7t8S8T8c8f8j8r9T9[9k9q9|:T:Y:`:i$]#sS%T%Z'Q'X'Z'a'c(h(w(x)R)V)^)_)k)q)x+X+g,O,S,j,u-h-i-|.y/p0`0b0g0i0w1`1y2a2h3R3c3d4c4d4j4m4s4u4y4z5d5p5w6U6e6i6s6z7q7r7t8S8T8c8f8j8r9T9[9k9q9|:T:Y:`:i$Z#tS%T%Z'Q'X'Z'a'c(h(w(x)R)^)_)k)q)x+X+g,O,S,j,u-h-i-|.y/p0`0b0g0i0w1`1y2a2h3R3c3d4c4d4j4m4s4u4y4z5d5p5w6U6e6i6s6z7q7r7t8S8T8c8f8j8r9T9[9k9q9|:T:Y:`:i$c#wS%T%Z'Q'X'Z'a'c(h(w(x)R)U)V)W)X)^)_)k)q)x+X+g,O,S,j,u-h-i-|.y/p0`0b0g0i0w1`1y2a2h3R3c3d4c4d4j4m4s4u4y4z5d5p5w6U6e6i6s6z7q7r7t8S8T8c8f8j8r9T9[9k9q9|:T:Y:`:i'X![O[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/[/c/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fQ/P(fQ/j)iQ4r/kR9d7y']![O[aefwx{!W!X!g!k!n!r!s!v!x#X#Y#[#g#i#l#q#r#s#t#u#v#w#x#y#z#}$U$W$Y$e$f$k%]%m&Q&S&W&b&f&x&y&|'O'P'b'i'j'y(`(b(i)l)r*h*i*l*q*r*v+W+Y+h+j+k,P,R,n,q,w-]-^-a-g.P.Q.U.}/Q/[/c/l/n/s/u0h0{1Q1a1b1l1p1z1|2c2f2i2u2z2}3i4O4R4W5Y5e5q6_6c6f6h6j6t6v6{7b7j7m8e8g8m8s8t9R9V9]9_9l9o9p9{:O:U:W:]:b:fQ#eQR(u#eU$|a;`;xb%Ue$e&f(b-Q1m1|2l8ZQ'_!u!Q*^$|%U'_*`*f+h,P0]0^1b2p6q6t7e8p9X9]:R;W;o;p;v;w<WS*`$}%SQ*f%XS+h&W1QQ,P&bQ0]*bQ0^*dQ1b+kQ2p-VS6q2q2sQ6t2uQ7e3wQ8p6rS9X7f7hQ9]7jQ:R9YQ;W%dS;o;X;YS;p<X<YQ;v;qQ;w;rT<W0z;a[[Owx!g&S*ql$c[&|'}+[,X,h,k-P-^-o-z._.d.ql&|!k%m&y'O,q2c6c6j8g9l9p:W:]:f^'}#R#S#T+b3[3]3^`+[&U&Y&c*v1S1T1f3iS,X&m-tQ,h&wU,k&x'P2}S-P'[2kW-^'b-]-a2zS-o't-wQ-z'xQ._(PS.d(Q.mR.q(UQ)|$[R/x)|Q0R)}Q4}/|Q5O/}Q5P0OY5Q0R4}5O5P8QR8Q5SQ*P$]S0U*P0VR0V*QS.`(P._S3n.`7[R7[3pQ3q.aS7Y3o3rU7^3q7Y8{R8{7ZQ.m(QR4S.m!|_O[wx!f!g!}#O$S$l&S&U&Y&c&m&x'P'b't(Q)p*q*v+[,X,k,w,z-]-^-a-o-t-w.d.m.o1S1T1f2f2z2}3i3tU$r_$u*]U$u`$d&rR*]$sU$}a;`;xd*a$}*b2q6r7f9Y;X;q;r<XQ*b%SQ2q-VQ6r2sQ7f3wQ9Y7hQ;X%dQ;q;YQ;r<YT<X0z;aS+{&a7iR1s+{S*j%Z/pR0c*jQ1U+_R5i1UU+e&V0z;xR1]+eQ+s&^Q1c+lT1i+s1cQ8[5yR9i8[QwOS&Rw&ST&Sx*qQ,`&qR2V,`W)u$W*q/n5YR/t)uU/o)q)v0gR4w/o[*u%e%f*W2`2g2{R0m*uQ,d&uR2Y,dQ-a'bQ2z-]T2|-a2zQ2w-XR6x2wQ-f'gQ2R,]T3Q-f2RS%pm7QR*z%pdnOwx!g&S't(Q*q-w.mR%unQ0t+RR5^0tQ.U'yR3f.UQ1v+}R6Q1vU*n%`*x;ZR0f*nS1g+n0nR5t1gQ7l3yQ9O7_U9a7l9O9}R9}9U$O!SO[_ewx!f!g!u!}#O#V#Z$S$T$e$l%U&S&U&Y&c&f&m&x'P'_'b't(Q(b(g)j)p*q*v+[+a+t,X,k,w,z-Q-]-^-a-o-t-w.d.m.o.u1S1T1X1f1k1m1|2f2l2z2}3i3t5j8ZR&g!SQ4Y.sR7v4YQ1},UR6X1}S/d)_)`R4o/dW(o#a(j(k/SR/W(oQ7{4fR9f7{T)a#}*v!USO[wx!g!k%m&S&y&|'O'b,q-]-^-a2c2z6c6j8g9l9p:W:]:fj$ya{$k%]+j,R1a1z5q6v8s9_:UY%Te$e(b1p3iY%Zf$f(i)l*lQ&j!WQ&k!XQ'Q!nQ'X!rQ'Z!sQ'a!vQ'c!xQ(a#XQ(e#YS(h#[+YQ(w#gQ(x#iQ)R#lQ)S#qQ)T#rQ)U#sQ)V#tQ)W#uQ)X#vQ)Y#wQ)Z#xQ)[#yQ)]#zQ)^#{S)_#}*vQ)k$UQ)q$WQ)x$YQ+X&QS+g&W1QQ,O&bQ,S&fQ,j&xQ,u'PQ-h'iQ-i'jS-|'y.UQ.y(`S/p)r0hS0`*h4aQ0b*iQ0g*qQ0i*rQ0w+WS1`+h+kQ1y,PQ2a,nS2h,w6{Q3R-gQ3c.PQ3d.QQ4c.}Q4d/QQ4j/[Q4m/cQ4s/lQ4u/nQ4y/sQ4z/uQ5d0{Q5p1bQ5w1lQ6U1|S6e2f8tQ6i2iQ6s2uQ6z2}Q7q4OQ7r4RQ7t4WQ8S5YQ8T5eQ8c6_Q8f6fQ8j6hQ8r6tS9T7b7mQ9[7jQ9k8eQ9q8mS9|9R9VQ:T9]Q:Y9oS:`9{:OR:i:bR,V&fd]Owx!g&S't(Q*q-w.m!v^[_`!f!}#O$S$d$l$s$u&U&Y&c&m&r&x'P'b)p*]*v+[,X,k,w,z-]-^-a-o-t.d.o1S1T1f2f2z2}3i3t#r${ae!u$e$|$}%S%U%X%d&W&b&f'_(b*`*b*d*f+h+k,P-Q-V0]0^1Q1b1m1|2l2p2q2s2u3w6q6r6t7e7f7h7j8Z8p9X9Y9]:R;W;X;Y;`;a;o;p;q;r;v;w<W<X<YQ%tnS+d&V+eW+r&^+l+s1cU+z&a+{7iQ1k+tT5c0z;x``Owx!g&S't*q-wS$d[-oQ$s_b%Xe$e&f(b-Q1m1|2l8Z!h&r!f!}#O$S$l&U&Y&c&m&x'P'b(Q)p*v+[,X,k,w,z-]-^-a-t.d.m.o1S1T1f2f2z2}3i3tQ'`!uS(_#V+aQ(f#ZS)i$T(gQ*d%UQ-T'_Q/k)jQ1j+tQ5k1XQ5v1kR8U5jS(W#R3]S(X#S3^V(Y#T+b3[R$^Ye0Q)}/|/}0O0R4}5O5P5S8QW(R#R#S#T+bQ([#US.Z'}(US.a(P._Q.x(^W1r+y.X5}6PQ3Z-tQ3h.WQ3o.`Q4V.qU7T3[3]3^Q7]3pR8|7[Q.b(PR3m._T.l(Q.mdgOwx!g&S&m't*q-t-wU$g[,X-oQ&s!fQ'k!}Q'u#OQ)h$SQ*Y$l`+]&U&Y&c*v1S1T1f3iQ,l&xQ,v'PY-_'b-]-a2z2}S.g(Q.mQ/m)pQ0y+[S2_,k-^S2j,w,zS3s.d.oQ6g2fR7c3td]Owx!g&S't(Q*q-w.m!v^[_`!f!}#O$S$d$l$s$u&U&Y&c&m&r&x'P'b)p*]*v+[,X,k,w,z-]-^-a-o-t.d.o1S1T1f2f2z2}3i3tR%tnQ3y.gQ7_3sQ7g3wQ7o3zQ7p3{Q9U7cU9W7e7f7hQ9`7kS:Q9X9YR:c:RZ+o&Y&c*v1S3ipzOnpwx!g%w&S't(Q*q-p-w.f.m.o[%Oa%d0z;`;a;xU%We%h1SQ%eg^&d{|%i1O5a;b;yQ's#OQ*W$gb*_$|$}%S;W;X;Y<W<X<YQ,b&sQ-x'uQ0Z*Y[0[*`*b;o;p;q;rQ0k*tQ0|+]Q2`,lQ2g,vS2{-_2_U5V0];v;wQ5b0yQ6l2jR8i6gQ+}&aR9Z7iS1q+y.XQ8^5}R8_6P[%^f$f(i)l)r0hR0d*lR+`&UQ+_&UR5h1TS&Xy+xQ*g%XU+f&V0z;xS+m&Y1SW+p&Z1O5a;yQ-j'kQ/h)hS0_*d*fQ1R+]Q1^+eQ4t/mQ5W0^Q5`0yQ5u1jR8X5vR5z1mYvOwx&S*qR&t!gW%gg,l,v-_T*X$g2_T)w$W*q[uOwx!g&S*qQ&}!kQ*y%mQ,p&yQ,t'OQ2d,qQ6b2cQ8d6cQ8l6jQ9n8gQ:X9lQ:[9pQ:e:WQ:g:]R:k:fxlOwx!g!k%m&S&y'O*q,q2c6c6j8g9l9p:W:]:fU$h[&|-^X-`'b-]-a2zQ-['`R2o-TS-X'`-TQ2r-VQ2x-YU6p2p2q2sQ6w2vS8o6q6rR9s8p[rOwx!g&S*qS-q't-wT.h(Q.mR+S%y[sOwx!g&S*qS-s't-wT.i(Q.m[tOwx!g&S*qT.j(Q.mT.T'y.UX%af%k0h1QQ.w([R4_.xR.t(ZR(d#XQ(r#aS/R(j(kR4e/SR/V(lR4g/T",
  nodeNames: "\u26A0 RawString > MacroName LineComment BlockComment PreprocDirective #include String EscapeSequence SystemLibString Identifier ) ( ArgumentList ConditionalExpression AssignmentExpression CallExpression PrimitiveType FieldExpression FieldIdentifier DestructorName TemplateMethod ScopedFieldIdentifier NamespaceIdentifier TemplateType TypeIdentifier ScopedTypeIdentifier ScopedNamespaceIdentifier :: NamespaceIdentifier TypeIdentifier TemplateArgumentList < TypeDescriptor const volatile restrict _Atomic mutable constexpr constinit consteval StructSpecifier struct MsDeclspecModifier __declspec Attribute AttributeName Identifier AttributeArgs { } [ ] UpdateOp ArithOp ArithOp ArithOp LogicOp BitOp BitOp BitOp CompareOp CompareOp CompareOp > CompareOp BitOp UpdateOp , Number CharLiteral AttributeArgs VirtualSpecifier BaseClassClause Access virtual FieldDeclarationList FieldDeclaration extern static register inline thread_local AttributeSpecifier __attribute__ PointerDeclarator MsBasedModifier __based MsPointerModifier FunctionDeclarator ParameterList ParameterDeclaration PointerDeclarator FunctionDeclarator Noexcept noexcept RequiresClause requires True False ParenthesizedExpression CommaExpression LambdaExpression LambdaCaptureSpecifier TemplateParameterList OptionalParameterDeclaration TypeParameterDeclaration typename class VariadicParameterDeclaration VariadicDeclarator ReferenceDeclarator OptionalTypeParameterDeclaration VariadicTypeParameterDeclaration TemplateTemplateParameterDeclaration template AbstractFunctionDeclarator AbstractPointerDeclarator AbstractArrayDeclarator AbstractParenthesizedDeclarator AbstractReferenceDeclarator ThrowSpecifier throw TrailingReturnType CompoundStatement FunctionDefinition MsCallModifier TryStatement try CatchClause catch LinkageSpecification Declaration InitDeclarator InitializerList InitializerPair SubscriptDesignator FieldDesignator ExportDeclaration export ImportDeclaration import ModuleName PartitionName HeaderName CaseStatement case default LabeledStatement StatementIdentifier ExpressionStatement IfStatement if ConditionClause Declaration else SwitchStatement switch DoStatement do while WhileStatement ForStatement for ReturnStatement return BreakStatement break ContinueStatement continue GotoStatement goto CoReturnStatement co_return CoYieldStatement co_yield AttributeStatement ForRangeLoop AliasDeclaration using TypeDefinition typedef PointerDeclarator FunctionDeclarator ArrayDeclarator ParenthesizedDeclarator ThrowStatement NamespaceDefinition namespace ScopedIdentifier Identifier OperatorName operator ArithOp BitOp CompareOp LogicOp new delete co_await ConceptDefinition concept UsingDeclaration enum StaticAssertDeclaration static_assert ConcatenatedString TemplateDeclaration FriendDeclaration friend union FunctionDefinition ExplicitFunctionSpecifier explicit FieldInitializerList FieldInitializer DefaultMethodClause DeleteMethodClause FunctionDefinition OperatorCast operator TemplateInstantiation FunctionDefinition FunctionDefinition Declaration ModuleDeclaration module RequiresExpression RequirementList SimpleRequirement TypeRequirement CompoundRequirement ReturnTypeRequirement ConstraintConjuction LogicOp ConstraintDisjunction LogicOp ArrayDeclarator ParenthesizedDeclarator ReferenceDeclarator TemplateFunction OperatorName StructuredBindingDeclarator ArrayDeclarator ParenthesizedDeclarator ReferenceDeclarator BitfieldClause FunctionDefinition FunctionDefinition Declaration FunctionDefinition Declaration AccessSpecifier UnionSpecifier ClassSpecifier EnumSpecifier SizedTypeSpecifier TypeSize EnumeratorList Enumerator DependentType Decltype decltype auto PlaceholderTypeSpecifier ParameterPackExpansion ParameterPackExpansion FieldIdentifier PointerExpression SubscriptExpression BinaryExpression ArithOp LogicOp LogicOp BitOp UnaryExpression LogicOp BitOp UpdateExpression CastExpression SizeofExpression sizeof CoAwaitExpression CompoundLiteralExpression NULL NewExpression new NewDeclarator DeleteExpression delete ParameterPackExpansion nullptr this UserDefinedLiteral ParamPack #define PreprocArg #if #ifdef #ifndef #else #endif #elif PreprocDirectiveName Macro Program",
  maxTerm: 425,
  nodeProps: [
    ["group", -35, 1, 8, 11, 15, 16, 17, 19, 71, 72, 100, 101, 102, 104, 191, 208, 229, 242, 243, 270, 271, 272, 277, 280, 281, 282, 284, 285, 286, 287, 290, 292, 293, 294, 295, 296, "Expression", -13, 18, 25, 26, 27, 43, 255, 256, 257, 258, 262, 263, 265, 266, "Type", -19, 126, 129, 147, 150, 152, 153, 158, 160, 163, 164, 166, 168, 170, 172, 174, 176, 178, 179, 188, "Statement"],
    ["isolate", -4, 4, 5, 8, 10, ""],
    ["openedBy", 12, "(", 52, "{", 54, "["],
    ["closedBy", 13, ")", 51, "}", 53, "]"]
  ],
  propSources: [cppHighlighting],
  skippedNodes: [0, 3, 4, 5, 6, 7, 10, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 347, 348],
  repeatNodeCount: 41,
  tokenData: "&*r7ZR!UOX$eXY({YZ.gZ]$e]^+P^p$epq({qr.}rs0}st2ktu$euv!7dvw!9bwx!;exy!<Yyz!=Tz{!>O{|!?R|}!AV}!O!BQ!O!P!DX!P!Q#+y!Q!R#Az!R![$(x![!]$Ag!]!^$Cc!^!_$D^!_!`%1W!`!a%2X!a!b%5_!b!c$e!c!n%6Y!n!o%7q!o!w%6Y!w!x%7q!x!}%6Y!}#O%:n#O#P%<g#P#Q%Kz#Q#R%Ms#R#S%6Y#S#T$e#T#i%6Y#i#j%Nv#j#o%6Y#o#p&!e#p#q&#`#q#r&%f#r#s&&a#s;'S$e;'S;=`(u<%lO$e&t$nY)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e&r%eW)]W'f&jOY%^Zw%^wx%}x#O%^#O#P&f#P;'S%^;'S;=`'x<%lO%^&j&SU'f&jOY%}Z#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}&j&kX'f&jOY%}YZ%}Z]%}]^'W^#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}&j']V'f&jOY%}YZ%}Z#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}&j'uP;=`<%l%}&r'{P;=`<%l%^&l(VW(qQ'f&jOY(OZr(Ors%}s#O(O#O#P&f#P;'S(O;'S;=`(o<%lO(O&l(rP;=`<%l(O&t(xP;=`<%l$e7Z)Y`)]W(qQ(n.o'f&j*Z)`OX$eXY({YZ*[Z]$e]^+P^p$epq({qr$ers%^sw$ewx(Ox#O$e#O#P,^#P;'S$e;'S;=`(u<%lO$e.o*aT(n.oXY*[YZ*[]^*[pq*[#O#P*p.o*sQYZ*[]^*y.o*|PYZ*[4e+[`)]W(qQ(n.o'f&jOX$eXY+PYZ*[Z]$e]^+P^p$epq+Pqr$ers%^sw$ewx(Ox#O$e#O#P,^#P;'S$e;'S;=`(u<%lO$e4Z,cX'f&jOY%}YZ-OZ]%}]^-{^#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}4Z-V[(n.o'f&jOX%}XY-OYZ*[Z]%}]^-O^p%}pq-Oq#O%}#O#P,^#P;'S%};'S;=`'r<%lO%}4Z.QV'f&jOY%}YZ-OZ#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}7P.nT*W)`(n.oXY*[YZ*[]^*[pq*[#O#P*p3o/[[%^!b'QP)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox!_$e!_!`0Q!`#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o0_Y%]!b!a,g)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e6e1YY)]W(oQ(p/]'f&jOY%^Zr%^rs1xsw%^wx%}x#O%^#O#P&f#P;'S%^;'S;=`'x<%lO%^(U2RW)x!b)]W'f&jOY%^Zw%^wx%}x#O%^#O#P&f#P;'S%^;'S;=`'x<%lO%^4e2tf)]W(qQ'f&jOX$eXY2kZp$epq2kqr$ers%^sw$ewx(Ox!c$e!c!}4Y!}#O$e#O#P&f#P#T$e#T#W4Y#W#X5m#X#Y>u#Y#]4Y#]#^NZ#^#o4Y#o;'S$e;'S;=`(u<%lO$e4e4eb)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#o4Y#o;'S$e;'S;=`(u<%lO$e4e5xd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#X4Y#X#Y7W#Y#o4Y#o;'S$e;'S;=`(u<%lO$e4e7cd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#Y4Y#Y#Z8q#Z#o4Y#o;'S$e;'S;=`(u<%lO$e4e8|d)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#]4Y#]#^:[#^#o4Y#o;'S$e;'S;=`(u<%lO$e4e:gd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#b4Y#b#c;u#c#o4Y#o;'S$e;'S;=`(u<%lO$e4e<Qd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#X4Y#X#Y=`#Y#o4Y#o;'S$e;'S;=`(u<%lO$e4e=mb)]W(qQ'e.o'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#o4Y#o;'S$e;'S;=`(u<%lO$e4e?Qf)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#`4Y#`#a@f#a#b4Y#b#cHV#c#o4Y#o;'S$e;'S;=`(u<%lO$e4e@qf)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#]4Y#]#^BV#^#g4Y#g#hEV#h#o4Y#o;'S$e;'S;=`(u<%lO$e4eBbd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#Y4Y#Y#ZCp#Z#o4Y#o;'S$e;'S;=`(u<%lO$e4eC}b)]W(qQ'f&j'l.o'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#o4Y#o;'S$e;'S;=`(u<%lO$e4eEbd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#X4Y#X#YFp#Y#o4Y#o;'S$e;'S;=`(u<%lO$e4eF}b)]W(qQ'j.o'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#o4Y#o;'S$e;'S;=`(u<%lO$e4eHbd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#W4Y#W#XIp#X#o4Y#o;'S$e;'S;=`(u<%lO$e4eI{d)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#]4Y#]#^KZ#^#o4Y#o;'S$e;'S;=`(u<%lO$e4eKfd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#Y4Y#Y#ZLt#Z#o4Y#o;'S$e;'S;=`(u<%lO$e4eMRb)]W(qQ'f&j'k.o'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#o4Y#o;'S$e;'S;=`(u<%lO$e4eNff)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#Y4Y#Y#Z! z#Z#b4Y#b#c!.[#c#o4Y#o;'S$e;'S;=`(u<%lO$e4e!!Xf)]W(qQ'g.o'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#W4Y#W#X!#m#X#b4Y#b#c!(W#c#o4Y#o;'S$e;'S;=`(u<%lO$e4e!#xd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#X4Y#X#Y!%W#Y#o4Y#o;'S$e;'S;=`(u<%lO$e4e!%cd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#Y4Y#Y#Z!&q#Z#o4Y#o;'S$e;'S;=`(u<%lO$e4e!'Ob)]W(qQ'h.o'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#o4Y#o;'S$e;'S;=`(u<%lO$e4e!(cd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#W4Y#W#X!)q#X#o4Y#o;'S$e;'S;=`(u<%lO$e4e!)|d)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#X4Y#X#Y!+[#Y#o4Y#o;'S$e;'S;=`(u<%lO$e4e!+gd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#Y4Y#Y#Z!,u#Z#o4Y#o;'S$e;'S;=`(u<%lO$e4e!-Sb)]W(qQ'i.o'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#o4Y#o;'S$e;'S;=`(u<%lO$e4e!.gd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#V4Y#V#W!/u#W#o4Y#o;'S$e;'S;=`(u<%lO$e4e!0Qd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#`4Y#`#a!1`#a#o4Y#o;'S$e;'S;=`(u<%lO$e4e!1kd)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#i4Y#i#j!2y#j#o4Y#o;'S$e;'S;=`(u<%lO$e4e!3Ud)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#W4Y#W#X!4d#X#o4Y#o;'S$e;'S;=`(u<%lO$e4e!4od)]W(qQ'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#X4Y#X#Y!5}#Y#o4Y#o;'S$e;'S;=`(u<%lO$e4e!6[b)]W(qQV.o'f&j'm.oOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![4Y![!c$e!c!}4Y!}#O$e#O#P&f#P#R$e#R#S4Y#S#T$e#T#o4Y#o;'S$e;'S;=`(u<%lO$e3o!7q[)]W(qQ%Z!b![,g'f&jOY$eZr$ers%^sw$ewx(Ox!_$e!_!`!8g!`#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o!8rY!g-y)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o!9o])W,g)]W(qQ%[!b'f&jOY$eZr$ers%^sv$evw!:hwx(Ox!_$e!_!`!8g!`#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o!:uY)V,g%^!b)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e2X!;pW)ZS(qQ)[,g'f&jOY(OZr(Ors%}s#O(O#O#P&f#P;'S(O;'S;=`(o<%lO(O6i!<eY)]W(qQ]6_'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e'V!=`Y[a)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o!>][)T,g)]W(qQ%Z!b'f&jOY$eZr$ers%^sw$ewx(Ox!_$e!_!`!8g!`#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o!?`^)]W(qQ%Z!b!Y,g'f&jOY$eZr$ers%^sw$ewx(Ox{$e{|!@[|!_$e!_!`!8g!`#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o!@gY)]W!X-y(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e2a!AbY!h,k)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o!B__)]W(qQ%Z!b!Y,g'f&jOY$eZr$ers%^sw$ewx(Ox}$e}!O!@[!O!_$e!_!`!8g!`!a!C^!a#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o!CiY(x-y)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e2a!Dd^)]W(qQ'f&j(w,gOY$eZr$ers%^sw$ewx(Ox!O$e!O!P!E`!P!Q$e!Q![!GY![#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e2a!Ei[)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox!O$e!O!P!F_!P#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e2a!FjY)Y,k)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e2]!Gen)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx!Icx!Q$e!Q![!GY![!g$e!g!h#$w!h!i#*Y!i!n$e!n!o#*Y!o!r$e!r!s#$w!s!w$e!w!x#*Y!x#O$e#O#P&f#P#X$e#X#Y#$w#Y#Z#*Y#Z#`$e#`#a#*Y#a#d$e#d#e#$w#e#i$e#i#j#*Y#j;'S$e;'S;=`(u<%lO$e2T!IjY(qQ'f&jOY(OZr(Ors%}s!Q(O!Q![!JY![#O(O#O#P&f#P;'S(O;'S;=`(o<%lO(O2T!Jcn(qQ!i,g'f&jOY(OZr(Ors%}sw(Owx!Icx!Q(O!Q![!JY![!g(O!g!h!La!h!i##`!i!n(O!n!o##`!o!r(O!r!s!La!s!w(O!w!x##`!x#O(O#O#P&f#P#X(O#X#Y!La#Y#Z##`#Z#`(O#`#a##`#a#d(O#d#e!La#e#i(O#i#j##`#j;'S(O;'S;=`(o<%lO(O2T!Ljl(qQ!i,g'f&jOY(OZr(Ors%}s{(O{|!Nb|}(O}!O!Nb!O!Q(O!Q![# e![!c(O!c!h# e!h!i# e!i!n(O!n!o##`!o!w(O!w!x##`!x#O(O#O#P&f#P#T(O#T#Y# e#Y#Z# e#Z#`(O#`#a##`#a#i(O#i#j##`#j;'S(O;'S;=`(o<%lO(O2T!Ni^(qQ'f&jOY(OZr(Ors%}s!Q(O!Q![# e![!c(O!c!i# e!i#O(O#O#P&f#P#T(O#T#Z# e#Z;'S(O;'S;=`(o<%lO(O2T# nj(qQ!i,g'f&jOY(OZr(Ors%}sw(Owx!Nbx!Q(O!Q![# e![!c(O!c!h# e!h!i# e!i!n(O!n!o##`!o!w(O!w!x##`!x#O(O#O#P&f#P#T(O#T#Y# e#Y#Z# e#Z#`(O#`#a##`#a#i(O#i#j##`#j;'S(O;'S;=`(o<%lO(O2T##id(qQ!i,g'f&jOY(OZr(Ors%}s!h(O!h!i##`!i!n(O!n!o##`!o!w(O!w!x##`!x#O(O#O#P&f#P#Y(O#Y#Z##`#Z#`(O#`#a##`#a#i(O#i#j##`#j;'S(O;'S;=`(o<%lO(O2]#%Sn)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx(Ox{$e{|#'Q|}$e}!O#'Q!O!Q$e!Q![#(]![!c$e!c!h#(]!h!i#(]!i!n$e!n!o#*Y!o!w$e!w!x#*Y!x#O$e#O#P&f#P#T$e#T#Y#(]#Y#Z#(]#Z#`$e#`#a#*Y#a#i$e#i#j#*Y#j;'S$e;'S;=`(u<%lO$e2]#'Z`)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![#(]![!c$e!c!i#(]!i#O$e#O#P&f#P#T$e#T#Z#(]#Z;'S$e;'S;=`(u<%lO$e2]#(hj)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx!Nbx!Q$e!Q![#(]![!c$e!c!h#(]!h!i#(]!i!n$e!n!o#*Y!o!w$e!w!x#*Y!x#O$e#O#P&f#P#T$e#T#Y#(]#Y#Z#(]#Z#`$e#`#a#*Y#a#i$e#i#j#*Y#j;'S$e;'S;=`(u<%lO$e2]#*ef)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx(Ox!h$e!h!i#*Y!i!n$e!n!o#*Y!o!w$e!w!x#*Y!x#O$e#O#P&f#P#Y$e#Y#Z#*Y#Z#`$e#`#a#*Y#a#i$e#i#j#*Y#j;'S$e;'S;=`(u<%lO$e7Z#,W`)]W(qQ%Z!b![,g'f&jOY$eZr$ers%^sw$ewx(Oxz$ez{#-Y{!P$e!P!Q#:s!Q!_$e!_!`!8g!`#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e7Z#-c])]W(qQ'f&jOY#-YYZ#.[Zr#-Yrs#/csw#-Ywx#5wxz#-Yz{#8j{#O#-Y#O#P#2`#P;'S#-Y;'S;=`#:m<%lO#-Y1e#._TOz#.[z{#.n{;'S#.[;'S;=`#/]<%lO#.[1e#.qVOz#.[z{#.n{!P#.[!P!Q#/W!Q;'S#.[;'S;=`#/]<%lO#.[1e#/]OT1e1e#/`P;=`<%l#.[7X#/jZ)]W'f&jOY#/cYZ#.[Zw#/cwx#0]xz#/cz{#4O{#O#/c#O#P#2`#P;'S#/c;'S;=`#5q<%lO#/c7P#0bX'f&jOY#0]YZ#.[Zz#0]z{#0}{#O#0]#O#P#2`#P;'S#0];'S;=`#3x<%lO#0]7P#1SZ'f&jOY#0]YZ#.[Zz#0]z{#0}{!P#0]!P!Q#1u!Q#O#0]#O#P#2`#P;'S#0];'S;=`#3x<%lO#0]7P#1|UT1e'f&jOY%}Z#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}7P#2eZ'f&jOY#0]YZ#0]Z]#0]]^#3W^z#0]z{#0}{#O#0]#O#P#2`#P;'S#0];'S;=`#3x<%lO#0]7P#3]X'f&jOY#0]YZ#0]Zz#0]z{#0}{#O#0]#O#P#2`#P;'S#0];'S;=`#3x<%lO#0]7P#3{P;=`<%l#0]7X#4V])]W'f&jOY#/cYZ#.[Zw#/cwx#0]xz#/cz{#4O{!P#/c!P!Q#5O!Q#O#/c#O#P#2`#P;'S#/c;'S;=`#5q<%lO#/c7X#5XW)]WT1e'f&jOY%^Zw%^wx%}x#O%^#O#P&f#P;'S%^;'S;=`'x<%lO%^7X#5tP;=`<%l#/c7R#6OZ(qQ'f&jOY#5wYZ#.[Zr#5wrs#0]sz#5wz{#6q{#O#5w#O#P#2`#P;'S#5w;'S;=`#8d<%lO#5w7R#6x](qQ'f&jOY#5wYZ#.[Zr#5wrs#0]sz#5wz{#6q{!P#5w!P!Q#7q!Q#O#5w#O#P#2`#P;'S#5w;'S;=`#8d<%lO#5w7R#7zW(qQT1e'f&jOY(OZr(Ors%}s#O(O#O#P&f#P;'S(O;'S;=`(o<%lO(O7R#8gP;=`<%l#5w7Z#8s_)]W(qQ'f&jOY#-YYZ#.[Zr#-Yrs#/csw#-Ywx#5wxz#-Yz{#8j{!P#-Y!P!Q#9r!Q#O#-Y#O#P#2`#P;'S#-Y;'S;=`#:m<%lO#-Y7Z#9}Y)]W(qQT1e'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e7Z#:pP;=`<%l#-Y7Z#;OY)]W(qQS1e'f&jOY#:sZr#:srs#;nsw#:swx#@{x#O#:s#O#P#<z#P;'S#:s;'S;=`#At<%lO#:s7X#;wW)]WS1e'f&jOY#;nZw#;nwx#<ax#O#;n#O#P#<z#P;'S#;n;'S;=`#@u<%lO#;n7P#<hUS1e'f&jOY#<aZ#O#<a#O#P#<z#P;'S#<a;'S;=`#>[<%lO#<a7P#=RXS1e'f&jOY#<aYZ%}Z]#<a]^#=n^#O#<a#O#P#>b#P;'S#<a;'S;=`#>[<%lO#<a7P#=uVS1e'f&jOY#<aYZ%}Z#O#<a#O#P#<z#P;'S#<a;'S;=`#>[<%lO#<a7P#>_P;=`<%l#<a7P#>i]S1e'f&jOY#<aYZ%}Z]#<a]^#=n^#O#<a#O#P#>b#P#b#<a#b#c#<a#c#f#<a#f#g#?b#g;'S#<a;'S;=`#>[<%lO#<a7P#?iUS1e'f&jOY#<aZ#O#<a#O#P#?{#P;'S#<a;'S;=`#>[<%lO#<a7P#@SZS1e'f&jOY#<aYZ%}Z]#<a]^#=n^#O#<a#O#P#>b#P#b#<a#b#c#<a#c;'S#<a;'S;=`#>[<%lO#<a7X#@xP;=`<%l#;n7R#AUW(qQS1e'f&jOY#@{Zr#@{rs#<as#O#@{#O#P#<z#P;'S#@{;'S;=`#An<%lO#@{7R#AqP;=`<%l#@{7Z#AwP;=`<%l#:s2]#BVt)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx#Dgx!O$e!O!P$ m!P!Q$e!Q![$(x![!g$e!g!h#$w!h!i#*Y!i!n$e!n!o#*Y!o!r$e!r!s#$w!s!w$e!w!x#*Y!x#O$e#O#P&f#P#U$e#U#V$+X#V#X$e#X#Y#$w#Y#Z#*Y#Z#`$e#`#a#*Y#a#d$e#d#e#$w#e#i$e#i#j#*Y#j#l$e#l#m$=`#m;'S$e;'S;=`(u<%lO$e2T#DnY(qQ'f&jOY(OZr(Ors%}s!Q(O!Q![#E^![#O(O#O#P&f#P;'S(O;'S;=`(o<%lO(O2T#Egp(qQ!i,g'f&jOY(OZr(Ors%}sw(Owx#Dgx!O(O!O!P#Gk!P!Q(O!Q![#E^![!g(O!g!h!La!h!i##`!i!n(O!n!o##`!o!r(O!r!s!La!s!w(O!w!x##`!x#O(O#O#P&f#P#X(O#X#Y!La#Y#Z##`#Z#`(O#`#a##`#a#d(O#d#e!La#e#i(O#i#j##`#j;'S(O;'S;=`(o<%lO(O2T#Gtn(qQ!i,g'f&jOY(OZr(Ors%}s!Q(O!Q![#Ir![!c(O!c!g#Ir!g!h#MS!h!i#Ir!i!n(O!n!o##`!o!r(O!r!s!La!s!w(O!w!x##`!x#O(O#O#P&f#P#T(O#T#X#Ir#X#Y#MS#Y#Z#Ir#Z#`(O#`#a##`#a#d(O#d#e!La#e#i(O#i#j##`#j;'S(O;'S;=`(o<%lO(O2T#I{p(qQ!i,g'f&jOY(OZr(Ors%}sw(Owx#LPx!Q(O!Q![#Ir![!c(O!c!g#Ir!g!h#MS!h!i#Ir!i!n(O!n!o##`!o!r(O!r!s!La!s!w(O!w!x##`!x#O(O#O#P&f#P#T(O#T#X#Ir#X#Y#MS#Y#Z#Ir#Z#`(O#`#a##`#a#d(O#d#e!La#e#i(O#i#j##`#j;'S(O;'S;=`(o<%lO(O2T#LW^(qQ'f&jOY(OZr(Ors%}s!Q(O!Q![#Ir![!c(O!c!i#Ir!i#O(O#O#P&f#P#T(O#T#Z#Ir#Z;'S(O;'S;=`(o<%lO(O2T#M]t(qQ!i,g'f&jOY(OZr(Ors%}sw(Owx#LPx{(O{|!Nb|}(O}!O!Nb!O!Q(O!Q![#Ir![!c(O!c!g#Ir!g!h#MS!h!i#Ir!i!n(O!n!o##`!o!r(O!r!s!La!s!w(O!w!x##`!x#O(O#O#P&f#P#T(O#T#X#Ir#X#Y#MS#Y#Z#Ir#Z#`(O#`#a##`#a#d(O#d#e!La#e#i(O#i#j##`#j;'S(O;'S;=`(o<%lO(O2]$ xp)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![$#|![!c$e!c!g$#|!g!h$&]!h!i$#|!i!n$e!n!o#*Y!o!r$e!r!s#$w!s!w$e!w!x#*Y!x#O$e#O#P&f#P#T$e#T#X$#|#X#Y$&]#Y#Z$#|#Z#`$e#`#a#*Y#a#d$e#d#e#$w#e#i$e#i#j#*Y#j;'S$e;'S;=`(u<%lO$e2]$$Xp)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx#LPx!Q$e!Q![$#|![!c$e!c!g$#|!g!h$&]!h!i$#|!i!n$e!n!o#*Y!o!r$e!r!s#$w!s!w$e!w!x#*Y!x#O$e#O#P&f#P#T$e#T#X$#|#X#Y$&]#Y#Z$#|#Z#`$e#`#a#*Y#a#d$e#d#e#$w#e#i$e#i#j#*Y#j;'S$e;'S;=`(u<%lO$e2]$&ht)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx#LPx{$e{|#'Q|}$e}!O#'Q!O!Q$e!Q![$#|![!c$e!c!g$#|!g!h$&]!h!i$#|!i!n$e!n!o#*Y!o!r$e!r!s#$w!s!w$e!w!x#*Y!x#O$e#O#P&f#P#T$e#T#X$#|#X#Y$&]#Y#Z$#|#Z#`$e#`#a#*Y#a#d$e#d#e#$w#e#i$e#i#j#*Y#j;'S$e;'S;=`(u<%lO$e2]$)Tp)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx#Dgx!O$e!O!P$ m!P!Q$e!Q![$(x![!g$e!g!h#$w!h!i#*Y!i!n$e!n!o#*Y!o!r$e!r!s#$w!s!w$e!w!x#*Y!x#O$e#O#P&f#P#X$e#X#Y#$w#Y#Z#*Y#Z#`$e#`#a#*Y#a#d$e#d#e#$w#e#i$e#i#j#*Y#j;'S$e;'S;=`(u<%lO$e2]$+b_)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox!O$e!O!P$,a!P!Q$e!Q!R$-`!R![$(x![#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e2]$,j[)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![!GY![#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e2]$-kt)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx#Dgx!O$e!O!P$ m!P!Q$e!Q![$(x![!g$e!g!h#$w!h!i#*Y!i!n$e!n!o#*Y!o!r$e!r!s#$w!s!w$e!w!x#*Y!x#O$e#O#P&f#P#U$e#U#V$/{#V#X$e#X#Y#$w#Y#Z#*Y#Z#`$e#`#a#*Y#a#d$e#d#e#$w#e#i$e#i#j#*Y#j#l$e#l#m$0z#m;'S$e;'S;=`(u<%lO$e2]$0U[)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![$(x![#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e2]$1T`)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![$2V![!c$e!c!i$2V!i#O$e#O#P&f#P#T$e#T#Z$2V#Z;'S$e;'S;=`(u<%lO$e2]$2br)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx$4lx!O$e!O!P$ m!P!Q$e!Q![$2V![!c$e!c!g$2V!g!h$:p!h!i$2V!i!n$e!n!o#*Y!o!r$e!r!s#$w!s!w$e!w!x#*Y!x#O$e#O#P&f#P#T$e#T#X$2V#X#Y$:p#Y#Z$2V#Z#`$e#`#a#*Y#a#d$e#d#e#$w#e#i$e#i#j#*Y#j;'S$e;'S;=`(u<%lO$e2T$4s^(qQ'f&jOY(OZr(Ors%}s!Q(O!Q![$5o![!c(O!c!i$5o!i#O(O#O#P&f#P#T(O#T#Z$5o#Z;'S(O;'S;=`(o<%lO(O2T$5xr(qQ!i,g'f&jOY(OZr(Ors%}sw(Owx$4lx!O(O!O!P#Gk!P!Q(O!Q![$5o![!c(O!c!g$5o!g!h$8S!h!i$5o!i!n(O!n!o##`!o!r(O!r!s!La!s!w(O!w!x##`!x#O(O#O#P&f#P#T(O#T#X$5o#X#Y$8S#Y#Z$5o#Z#`(O#`#a##`#a#d(O#d#e!La#e#i(O#i#j##`#j;'S(O;'S;=`(o<%lO(O2T$8]u(qQ!i,g'f&jOY(OZr(Ors%}sw(Owx$4lx{(O{|!Nb|}(O}!O!Nb!O!P#Gk!P!Q(O!Q![$5o![!c(O!c!g$5o!g!h$8S!h!i$5o!i!n(O!n!o##`!o!r(O!r!s!La!s!w(O!w!x##`!x#O(O#O#P&f#P#T(O#T#X$5o#X#Y$8S#Y#Z$5o#Z#`(O#`#a##`#a#d(O#d#e!La#e#i(O#i#j##`#j;'S(O;'S;=`(o<%lO(O2]$:{u)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx$4lx{$e{|#'Q|}$e}!O#'Q!O!P$ m!P!Q$e!Q![$2V![!c$e!c!g$2V!g!h$:p!h!i$2V!i!n$e!n!o#*Y!o!r$e!r!s#$w!s!w$e!w!x#*Y!x#O$e#O#P&f#P#T$e#T#X$2V#X#Y$:p#Y#Z$2V#Z#`$e#`#a#*Y#a#d$e#d#e#$w#e#i$e#i#j#*Y#j;'S$e;'S;=`(u<%lO$e2]$=ic)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox!O$e!O!P$,a!P!Q$e!Q!R$>t!R![$2V![!c$e!c!i$2V!i#O$e#O#P&f#P#T$e#T#Z$2V#Z;'S$e;'S;=`(u<%lO$e2]$?Pv)]W(qQ!i,g'f&jOY$eZr$ers%^sw$ewx$4lx!O$e!O!P$ m!P!Q$e!Q![$2V![!c$e!c!g$2V!g!h$:p!h!i$2V!i!n$e!n!o#*Y!o!r$e!r!s#$w!s!w$e!w!x#*Y!x#O$e#O#P&f#P#T$e#T#U$2V#U#V$2V#V#X$2V#X#Y$:p#Y#Z$2V#Z#`$e#`#a#*Y#a#d$e#d#e#$w#e#i$e#i#j#*Y#j#l$e#l#m$0z#m;'S$e;'S;=`(u<%lO$e4e$Ar[(v-X)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox![$e![!]$Bh!]#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3s$BsYm-})]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e2]$CnY)X,g)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e7V$Dk_q,g%]!b)]W(qQ'f&jOY$EjYZ$FlZr$Ejrs$GZsw$Ejwx%)Px!^$Ej!^!_%+w!_!`%.U!`!a%0]!a#O$Ej#O#P$Ib#P;'S$Ej;'S;=`%+q<%lO$Ej*[$Es])]W(qQ'f&jOY$EjYZ$FlZr$Ejrs$GZsw$Ejwx%)Px!`$Ej!`!a%*t!a#O$Ej#O#P$Ib#P;'S$Ej;'S;=`%+q<%lO$Ejp$FoTO!`$Fl!`!a$GO!a;'S$Fl;'S;=`$GT<%lO$Flp$GTO$Wpp$GWP;=`<%l$Fl*Y$GbZ)]W'f&jOY$GZYZ$FlZw$GZwx$HTx!`$GZ!`!a%(U!a#O$GZ#O#P$Ib#P;'S$GZ;'S;=`%(y<%lO$GZ*Q$HYX'f&jOY$HTYZ$FlZ!`$HT!`!a$Hu!a#O$HT#O#P$Ib#P;'S$HT;'S;=`$Mx<%lO$HT*Q$IOU$WpY#t'f&jOY%}Z#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}*Q$Ig['f&jOY$HTYZ$HTZ]$HT]^$J]^!`$HT!`!a$NO!a#O$HT#O#P%&n#P;'S$HT;'S;=`%'f;=`<%l%$z<%lO$HT*Q$JbX'f&jOY$HTYZ$J}Z!`$HT!`!a$Hu!a#O$HT#O#P$Ib#P;'S$HT;'S;=`$Mx<%lO$HT'[$KSX'f&jOY$J}YZ$FlZ!`$J}!`!a$Ko!a#O$J}#O#P$LY#P;'S$J};'S;=`$Mr<%lO$J}'[$KvU$Wp'f&jOY%}Z#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}'[$L_Z'f&jOY$J}YZ$J}Z]$J}]^$MQ^!`$J}!`!a$Ko!a#O$J}#O#P$LY#P;'S$J};'S;=`$Mr<%lO$J}'[$MVX'f&jOY$J}YZ$J}Z!`$J}!`!a$Ko!a#O$J}#O#P$LY#P;'S$J};'S;=`$Mr<%lO$J}'[$MuP;=`<%l$J}*Q$M{P;=`<%l$HT*Q$NVW$Wp'f&jOY$NoZ!`$No!`!a% ^!a#O$No#O#P% w#P;'S$No;'S;=`%#^<%lO$No)`$NtW'f&jOY$NoZ!`$No!`!a% ^!a#O$No#O#P% w#P;'S$No;'S;=`%#^<%lO$No)`% eUY#t'f&jOY%}Z#O%}#O#P&f#P;'S%};'S;=`'r<%lO%})`% |Y'f&jOY$NoYZ$NoZ]$No]^%!l^#O$No#O#P%#d#P;'S$No;'S;=`%$[;=`<%l%$z<%lO$No)`%!qX'f&jOY$NoYZ%}Z!`$No!`!a% ^!a#O$No#O#P% w#P;'S$No;'S;=`%#^<%lO$No)`%#aP;=`<%l$No)`%#iZ'f&jOY$NoYZ%}Z]$No]^%!l^!`$No!`!a% ^!a#O$No#O#P% w#P;'S$No;'S;=`%#^<%lO$No)`%$_XOY%$zZ!`%$z!`!a%%g!a#O%$z#O#P%%l#P;'S%$z;'S;=`%&h;=`<%l$No<%lO%$z#t%$}WOY%$zZ!`%$z!`!a%%g!a#O%$z#O#P%%l#P;'S%$z;'S;=`%&h<%lO%$z#t%%lOY#t#t%%oRO;'S%$z;'S;=`%%x;=`O%$z#t%%{XOY%$zZ!`%$z!`!a%%g!a#O%$z#O#P%%l#P;'S%$z;'S;=`%&h;=`<%l%$z<%lO%$z#t%&kP;=`<%l%$z*Q%&sZ'f&jOY$HTYZ$J}Z]$HT]^$J]^!`$HT!`!a$Hu!a#O$HT#O#P$Ib#P;'S$HT;'S;=`$Mx<%lO$HT*Q%'iXOY%$zZ!`%$z!`!a%%g!a#O%$z#O#P%%l#P;'S%$z;'S;=`%&h;=`<%l$HT<%lO%$z*Y%(aW$WpY#t)]W'f&jOY%^Zw%^wx%}x#O%^#O#P&f#P;'S%^;'S;=`'x<%lO%^*Y%(|P;=`<%l$GZ*S%)WZ(qQ'f&jOY%)PYZ$FlZr%)Prs$HTs!`%)P!`!a%)y!a#O%)P#O#P$Ib#P;'S%)P;'S;=`%*n<%lO%)P*S%*UW$WpY#t(qQ'f&jOY(OZr(Ors%}s#O(O#O#P&f#P;'S(O;'S;=`(o<%lO(O*S%*qP;=`<%l%)P*[%+RY$WpY#t)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e*[%+tP;=`<%l$Ej7V%,U^)]W(qQ%[!b!f,g'f&jOY$EjYZ$FlZr$Ejrs$GZsw$Ejwx%)Px!_$Ej!_!`%-Q!`!a%*t!a#O$Ej#O#P$Ib#P;'S$Ej;'S;=`%+q<%lO$Ej7V%-]]!g-y)]W(qQ'f&jOY$EjYZ$FlZr$Ejrs$GZsw$Ejwx%)Px!`$Ej!`!a%*t!a#O$Ej#O#P$Ib#P;'S$Ej;'S;=`%+q<%lO$Ej7V%.c]%]!b!b,g)]W(qQ'f&jOY$EjYZ$FlZr$Ejrs$GZsw$Ejwx%)Px!`$Ej!`!a%/[!a#O$Ej#O#P$Ib#P;'S$Ej;'S;=`%+q<%lO$Ej7V%/mY%]!b!b,g$WpY#t)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e)j%0hYY#t)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o%1c[)j!c)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox!_$e!_!`0Q!`#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o%2f]%]!b)]W(qQ!d,g'f&jOY$eZr$ers%^sw$ewx(Ox!_$e!_!`%3_!`!a%4[!a#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o%3lY%]!b!b,g)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o%4i[)]W(qQ%[!b!f,g'f&jOY$eZr$ers%^sw$ewx(Ox!_$e!_!`!8g!`#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e&u%5jY(uP)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e7Z%6ib)]W(yS(qQ!R,f(r%y'f&jOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![%6Y![!c$e!c!}%6Y!}#O$e#O#P&f#P#R$e#R#S%6Y#S#T$e#T#o%6Y#o;'S$e;'S;=`(u<%lO$e7Z%8Qb)]W(yS(qQ!R,f(r%y'f&jOY$eZr$ers%9Ysw$ewx%9{x!Q$e!Q![%6Y![!c$e!c!}%6Y!}#O$e#O#P&f#P#R$e#R#S%6Y#S#T$e#T#o%6Y#o;'S$e;'S;=`(u<%lO$e5P%9cW)]W(p/]'f&jOY%^Zw%^wx%}x#O%^#O#P&f#P;'S%^;'S;=`'x<%lO%^2T%:UW(qQ)[,g'f&jOY(OZr(Ors%}s#O(O#O#P&f#P;'S(O;'S;=`(o<%lO(O3o%:yZ!V-y)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox!}$e!}#O%;l#O#P&f#P;'S$e;'S;=`(u<%lO$e&u%;wY)QP)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e4e%<la'f&jOY%=qYZ%>[Z]%=q]^%?Z^!Q%=q!Q![%?w![!w%=q!w!x%AX!x#O%=q#O#P%H_#P#i%=q#i#j%Ds#j#l%=q#l#m%IR#m;'S%=q;'S;=`%Kt<%lO%=q&t%=xUXY'f&jOY%}Z#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}4e%>e[XY(n.o'f&jOX%}XY-OYZ*[Z]%}]^-O^p%}pq-Oq#O%}#O#P,^#P;'S%};'S;=`'r<%lO%}4e%?bVXY'f&jOY%}YZ-OZ#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}&t%@OWXY'f&jOY%}Z!Q%}!Q![%@h![#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}&t%@oWXY'f&jOY%}Z!Q%}!Q![%=q![#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}&t%A^['f&jOY%}Z!Q%}!Q![%BS![!c%}!c!i%BS!i#O%}#O#P&f#P#T%}#T#Z%BS#Z;'S%};'S;=`'r<%lO%}&t%BX['f&jOY%}Z!Q%}!Q![%B}![!c%}!c!i%B}!i#O%}#O#P&f#P#T%}#T#Z%B}#Z;'S%};'S;=`'r<%lO%}&t%CS['f&jOY%}Z!Q%}!Q![%Cx![!c%}!c!i%Cx!i#O%}#O#P&f#P#T%}#T#Z%Cx#Z;'S%};'S;=`'r<%lO%}&t%C}['f&jOY%}Z!Q%}!Q![%Ds![!c%}!c!i%Ds!i#O%}#O#P&f#P#T%}#T#Z%Ds#Z;'S%};'S;=`'r<%lO%}&t%Dx['f&jOY%}Z!Q%}!Q![%En![!c%}!c!i%En!i#O%}#O#P&f#P#T%}#T#Z%En#Z;'S%};'S;=`'r<%lO%}&t%Es['f&jOY%}Z!Q%}!Q![%Fi![!c%}!c!i%Fi!i#O%}#O#P&f#P#T%}#T#Z%Fi#Z;'S%};'S;=`'r<%lO%}&t%Fn['f&jOY%}Z!Q%}!Q![%Gd![!c%}!c!i%Gd!i#O%}#O#P&f#P#T%}#T#Z%Gd#Z;'S%};'S;=`'r<%lO%}&t%Gi['f&jOY%}Z!Q%}!Q![%=q![!c%}!c!i%=q!i#O%}#O#P&f#P#T%}#T#Z%=q#Z;'S%};'S;=`'r<%lO%}&t%HfXXY'f&jOY%}YZ%}Z]%}]^'W^#O%}#O#P&f#P;'S%};'S;=`'r<%lO%}&t%IW['f&jOY%}Z!Q%}!Q![%I|![!c%}!c!i%I|!i#O%}#O#P&f#P#T%}#T#Z%I|#Z;'S%};'S;=`'r<%lO%}&t%JR['f&jOY%}Z!Q%}!Q![%Jw![!c%}!c!i%Jw!i#O%}#O#P&f#P#T%}#T#Z%Jw#Z;'S%};'S;=`'r<%lO%}&t%KO[XY'f&jOY%}Z!Q%}!Q![%Jw![!c%}!c!i%Jw!i#O%}#O#P&f#P#T%}#T#Z%Jw#Z;'S%};'S;=`'r<%lO%}&t%KwP;=`<%l%=q2a%LVZ!W,V)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P#Q%Lx#Q;'S$e;'S;=`(u<%lO$e'Y%MTY)Pd)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o%NQ[)]W(qQ%[!b'f&j!_,gOY$eZr$ers%^sw$ewx(Ox!_$e!_!`!8g!`#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e7Z& Vd)]W(yS(qQ!R,f(r%y'f&jOY$eZr$ers%9Ysw$ewx%9{x!Q$e!Q!Y%6Y!Y!Z%7q!Z![%6Y![!c$e!c!}%6Y!}#O$e#O#P&f#P#R$e#R#S%6Y#S#T$e#T#o%6Y#o;'S$e;'S;=`(u<%lO$e2]&!pY!T,g)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e3o&#m^)]W(qQ%[!b'f&j!^,gOY$eZr$ers%^sw$ewx(Ox!_$e!_!`!8g!`#O$e#O#P&f#P#p$e#p#q&$i#q;'S$e;'S;=`(u<%lO$e3o&$vY)U,g%^!b)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e'V&%qY!Ua)]W(qQ'f&jOY$eZr$ers%^sw$ewx(Ox#O$e#O#P&f#P;'S$e;'S;=`(u<%lO$e(]&&nc)]W(qQ%[!b'RP'f&jOX$eXY&'yZp$epq&'yqr$ers%^sw$ewx(Ox!c$e!c!}&)_!}#O$e#O#P&f#P#R$e#R#S&)_#S#T$e#T#o&)_#o;'S$e;'S;=`(u<%lO$e&y&(Sc)]W(qQ'f&jOX$eXY&'yZp$epq&'yqr$ers%^sw$ewx(Ox!c$e!c!}&)_!}#O$e#O#P&f#P#R$e#R#S&)_#S#T$e#T#o&)_#o;'S$e;'S;=`(u<%lO$e&y&)jb)]W(qQeT'f&jOY$eZr$ers%^sw$ewx(Ox!Q$e!Q![&)_![!c$e!c!}&)_!}#O$e#O#P&f#P#R$e#R#S&)_#S#T$e#T#o&)_#o;'S$e;'S;=`(u<%lO$e",
  tokenizers: [rawString, fallback, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
  topRules: { "Program": [0, 307] },
  dynamicPrecedences: { "87": 1, "94": 1, "119": 1, "184": 1, "187": -10, "240": -10, "241": 1, "244": -1, "246": -10, "247": 1, "262": -1, "267": 2, "268": 2, "306": -10, "365": 3, "417": 1, "418": 3, "419": 1, "420": 1 },
  specialized: [{ term: 356, get: (value) => spec_identifier[value] || -1 }, { term: 33, get: (value) => spec_[value] || -1 }, { term: 66, get: (value) => spec_templateArgsEnd[value] || -1 }, { term: 363, get: (value) => spec_scopedIdentifier[value] || -1 }],
  tokenPrec: 24891
});

// node_modules/@marijn/find-cluster-break/src/index.js
var rangeFrom = [];
var rangeTo = [];
(() => {
  let numbers = "lc,34,7n,7,7b,19,,,,2,,2,,,20,b,1c,l,g,,2t,7,2,6,2,2,,4,z,,u,r,2j,b,1m,9,9,,o,4,,9,,3,,5,17,3,3b,f,,w,1j,,,,4,8,4,,3,7,a,2,t,,1m,,,,2,4,8,,9,,a,2,q,,2,2,1l,,4,2,4,2,2,3,3,,u,2,3,,b,2,1l,,4,5,,2,4,,k,2,m,6,,,1m,,,2,,4,8,,7,3,a,2,u,,1n,,,,c,,9,,14,,3,,1l,3,5,3,,4,7,2,b,2,t,,1m,,2,,2,,3,,5,2,7,2,b,2,s,2,1l,2,,,2,4,8,,9,,a,2,t,,20,,4,,2,3,,,8,,29,,2,7,c,8,2q,,2,9,b,6,22,2,r,,,,,,1j,e,,5,,2,5,b,,10,9,,2u,4,,6,,2,2,2,p,2,4,3,g,4,d,,2,2,6,,f,,jj,3,qa,3,t,3,t,2,u,2,1s,2,,7,8,,2,b,9,,19,3,3b,2,y,,3a,3,4,2,9,,6,3,63,2,2,,1m,,,7,,,,,2,8,6,a,2,,1c,h,1r,4,1c,7,,,5,,14,9,c,2,w,4,2,2,,3,1k,,,2,3,,,3,1m,8,2,2,48,3,,d,,7,4,,6,,3,2,5i,1m,,5,ek,,5f,x,2da,3,3x,,2o,w,fe,6,2x,2,n9w,4,,a,w,2,28,2,7k,,3,,4,,p,2,5,,47,2,q,i,d,,12,8,p,b,1a,3,1c,,2,4,2,2,13,,1v,6,2,2,2,2,c,,8,,1b,,1f,,,3,2,2,5,2,,,16,2,8,,6m,,2,,4,,fn4,,kh,g,g,g,a6,2,gt,,6a,,45,5,1ae,3,,2,5,4,14,3,4,,4l,2,fx,4,ar,2,49,b,4w,,1i,f,1k,3,1d,4,2,2,1x,3,10,5,,8,1q,,c,2,1g,9,a,4,2,,2n,3,2,,,2,6,,4g,,3,8,l,2,1l,2,,,,,m,,e,7,3,5,5f,8,2,3,,,n,,29,,2,6,,,2,,,2,,2,6j,,2,4,6,2,,2,r,2,2d,8,2,,,2,2y,,,,2,6,,,2t,3,2,4,,5,77,9,,2,6t,,a,2,,,4,,40,4,2,2,4,,w,a,14,6,2,4,8,,9,6,2,3,1a,d,,2,ba,7,,6,,,2a,m,2,7,,2,,2,3e,6,3,,,2,,7,,,20,2,3,,,,9n,2,f0b,5,1n,7,t4,,1r,4,29,,f5k,2,43q,,,3,4,5,8,8,2,7,u,4,44,3,1iz,1j,4,1e,8,,e,,m,5,,f,11s,7,,h,2,7,,2,,5,79,7,c5,4,15s,7,31,7,240,5,gx7k,2o,3k,6o".split(",").map((s) => s ? parseInt(s, 36) : 1);
  for (let i = 0, n = 0; i < numbers.length; i++)
    (i % 2 ? rangeTo : rangeFrom).push(n = n + numbers[i]);
})();
function isExtendingChar(code) {
  if (code < 768) return false;
  for (let from = 0, to = rangeFrom.length; ; ) {
    let mid = from + to >> 1;
    if (code < rangeFrom[mid]) to = mid;
    else if (code >= rangeTo[mid]) from = mid + 1;
    else return true;
    if (from == to) return false;
  }
}
function isRegionalIndicator(code) {
  return code >= 127462 && code <= 127487;
}
var ZWJ = 8205;
function findClusterBreak(str, pos, forward = true, includeExtending = true) {
  return (forward ? nextClusterBreak : prevClusterBreak)(str, pos, includeExtending);
}
function nextClusterBreak(str, pos, includeExtending) {
  if (pos == str.length) return pos;
  if (pos && surrogateLow(str.charCodeAt(pos)) && surrogateHigh(str.charCodeAt(pos - 1))) pos--;
  let prev = codePointAt(str, pos);
  pos += codePointSize(prev);
  while (pos < str.length) {
    let next = codePointAt(str, pos);
    if (prev == ZWJ || next == ZWJ || includeExtending && isExtendingChar(next)) {
      pos += codePointSize(next);
      prev = next;
    } else if (isRegionalIndicator(next)) {
      let countBefore = 0, i = pos - 2;
      while (i >= 0 && isRegionalIndicator(codePointAt(str, i))) {
        countBefore++;
        i -= 2;
      }
      if (countBefore % 2 == 0) break;
      else pos += 2;
    } else {
      break;
    }
  }
  return pos;
}
function prevClusterBreak(str, pos, includeExtending) {
  while (pos > 0) {
    let found = nextClusterBreak(str, pos - 2, includeExtending);
    if (found < pos) return found;
    pos--;
  }
  return 0;
}
function codePointAt(str, pos) {
  let code0 = str.charCodeAt(pos);
  if (!surrogateHigh(code0) || pos + 1 == str.length) return code0;
  let code1 = str.charCodeAt(pos + 1);
  if (!surrogateLow(code1)) return code0;
  return (code0 - 55296 << 10) + (code1 - 56320) + 65536;
}
function surrogateLow(ch) {
  return ch >= 56320 && ch < 57344;
}
function surrogateHigh(ch) {
  return ch >= 55296 && ch < 56320;
}
function codePointSize(code) {
  return code < 65536 ? 1 : 2;
}

// node_modules/@codemirror/state/dist/index.js
var Text = class _Text {
  /**
  Get the line description around the given position.
  */
  lineAt(pos) {
    if (pos < 0 || pos > this.length)
      throw new RangeError(`Invalid position ${pos} in document of length ${this.length}`);
    return this.lineInner(pos, false, 1, 0);
  }
  /**
  Get the description for the given (1-based) line number.
  */
  line(n) {
    if (n < 1 || n > this.lines)
      throw new RangeError(`Invalid line number ${n} in ${this.lines}-line document`);
    return this.lineInner(n, true, 1, 0);
  }
  /**
  Replace a range of the text with the given content.
  */
  replace(from, to, text) {
    [from, to] = clip(this, from, to);
    let parts = [];
    this.decompose(
      0,
      from,
      parts,
      2
      /* Open.To */
    );
    if (text.length)
      text.decompose(
        0,
        text.length,
        parts,
        1 | 2
        /* Open.To */
      );
    this.decompose(
      to,
      this.length,
      parts,
      1
      /* Open.From */
    );
    return TextNode.from(parts, this.length - (to - from) + text.length);
  }
  /**
  Append another document to this one.
  */
  append(other) {
    return this.replace(this.length, this.length, other);
  }
  /**
  Retrieve the text between the given points.
  */
  slice(from, to = this.length) {
    [from, to] = clip(this, from, to);
    let parts = [];
    this.decompose(from, to, parts, 0);
    return TextNode.from(parts, to - from);
  }
  /**
  Test whether this text is equal to another instance.
  */
  eq(other) {
    if (other == this)
      return true;
    if (other.length != this.length || other.lines != this.lines)
      return false;
    let start = this.scanIdentical(other, 1), end = this.length - this.scanIdentical(other, -1);
    let a2 = new RawTextCursor(this), b = new RawTextCursor(other);
    for (let skip = start, pos = start; ; ) {
      a2.next(skip);
      b.next(skip);
      skip = 0;
      if (a2.lineBreak != b.lineBreak || a2.done != b.done || a2.value != b.value)
        return false;
      pos += a2.value.length;
      if (a2.done || pos >= end)
        return true;
    }
  }
  /**
  Iterate over the text. When `dir` is `-1`, iteration happens
  from end to start. This will return lines and the breaks between
  them as separate strings.
  */
  iter(dir = 1) {
    return new RawTextCursor(this, dir);
  }
  /**
  Iterate over a range of the text. When `from` > `to`, the
  iterator will run in reverse.
  */
  iterRange(from, to = this.length) {
    return new PartialTextCursor(this, from, to);
  }
  /**
  Return a cursor that iterates over the given range of lines,
  _without_ returning the line breaks between, and yielding empty
  strings for empty lines.
  
  When `from` and `to` are given, they should be 1-based line numbers.
  */
  iterLines(from, to) {
    let inner;
    if (from == null) {
      inner = this.iter();
    } else {
      if (to == null)
        to = this.lines + 1;
      let start = this.line(from).from;
      inner = this.iterRange(start, Math.max(start, to == this.lines + 1 ? this.length : to <= 1 ? 0 : this.line(to - 1).to));
    }
    return new LineCursor(inner);
  }
  /**
  Return the document as a string, using newline characters to
  separate lines.
  */
  toString() {
    return this.sliceString(0);
  }
  /**
  Convert the document to an array of lines (which can be
  deserialized again via [`Text.of`](https://codemirror.net/6/docs/ref/#state.Text^of)).
  */
  toJSON() {
    let lines = [];
    this.flatten(lines);
    return lines;
  }
  /**
  @internal
  */
  constructor() {
  }
  /**
  Create a `Text` instance for the given array of lines.
  */
  static of(text) {
    if (text.length == 0)
      throw new RangeError("A document must have at least one line");
    if (text.length == 1 && !text[0])
      return _Text.empty;
    return text.length <= 32 ? new TextLeaf(text) : TextNode.from(TextLeaf.split(text, []));
  }
};
var TextLeaf = class _TextLeaf extends Text {
  constructor(text, length = textLength(text)) {
    super();
    this.text = text;
    this.length = length;
  }
  get lines() {
    return this.text.length;
  }
  get children() {
    return null;
  }
  lineInner(target, isLine, line, offset) {
    for (let i = 0; ; i++) {
      let string2 = this.text[i], end = offset + string2.length;
      if ((isLine ? line : end) >= target)
        return new Line(offset, end, line, string2);
      offset = end + 1;
      line++;
    }
  }
  decompose(from, to, target, open) {
    let text = from <= 0 && to >= this.length ? this : new _TextLeaf(sliceText(this.text, from, to), Math.min(to, this.length) - Math.max(0, from));
    if (open & 1) {
      let prev = target.pop();
      let joined = appendText(text.text, prev.text.slice(), 0, text.length);
      if (joined.length <= 32) {
        target.push(new _TextLeaf(joined, prev.length + text.length));
      } else {
        let mid = joined.length >> 1;
        target.push(new _TextLeaf(joined.slice(0, mid)), new _TextLeaf(joined.slice(mid)));
      }
    } else {
      target.push(text);
    }
  }
  replace(from, to, text) {
    if (!(text instanceof _TextLeaf))
      return super.replace(from, to, text);
    [from, to] = clip(this, from, to);
    let lines = appendText(this.text, appendText(text.text, sliceText(this.text, 0, from)), to);
    let newLen = this.length + text.length - (to - from);
    if (lines.length <= 32)
      return new _TextLeaf(lines, newLen);
    return TextNode.from(_TextLeaf.split(lines, []), newLen);
  }
  sliceString(from, to = this.length, lineSep = "\n") {
    [from, to] = clip(this, from, to);
    let result = "";
    for (let pos = 0, i = 0; pos <= to && i < this.text.length; i++) {
      let line = this.text[i], end = pos + line.length;
      if (pos > from && i)
        result += lineSep;
      if (from < end && to > pos)
        result += line.slice(Math.max(0, from - pos), to - pos);
      pos = end + 1;
    }
    return result;
  }
  flatten(target) {
    for (let line of this.text)
      target.push(line);
  }
  scanIdentical() {
    return 0;
  }
  static split(text, target) {
    let part = [], len = -1;
    for (let line of text) {
      part.push(line);
      len += line.length + 1;
      if (part.length == 32) {
        target.push(new _TextLeaf(part, len));
        part = [];
        len = -1;
      }
    }
    if (len > -1)
      target.push(new _TextLeaf(part, len));
    return target;
  }
};
var TextNode = class _TextNode extends Text {
  constructor(children, length) {
    super();
    this.children = children;
    this.length = length;
    this.lines = 0;
    for (let child of children)
      this.lines += child.lines;
  }
  lineInner(target, isLine, line, offset) {
    for (let i = 0; ; i++) {
      let child = this.children[i], end = offset + child.length, endLine = line + child.lines - 1;
      if ((isLine ? endLine : end) >= target)
        return child.lineInner(target, isLine, line, offset);
      offset = end + 1;
      line = endLine + 1;
    }
  }
  decompose(from, to, target, open) {
    for (let i = 0, pos = 0; pos <= to && i < this.children.length; i++) {
      let child = this.children[i], end = pos + child.length;
      if (from <= end && to >= pos) {
        let childOpen = open & ((pos <= from ? 1 : 0) | (end >= to ? 2 : 0));
        if (pos >= from && end <= to && !childOpen)
          target.push(child);
        else
          child.decompose(from - pos, to - pos, target, childOpen);
      }
      pos = end + 1;
    }
  }
  replace(from, to, text) {
    [from, to] = clip(this, from, to);
    if (text.lines < this.lines)
      for (let i = 0, pos = 0; i < this.children.length; i++) {
        let child = this.children[i], end = pos + child.length;
        if (from >= pos && to <= end) {
          let updated = child.replace(from - pos, to - pos, text);
          let totalLines = this.lines - child.lines + updated.lines;
          if (updated.lines < totalLines >> 5 - 1 && updated.lines > totalLines >> 5 + 1) {
            let copy = this.children.slice();
            copy[i] = updated;
            return new _TextNode(copy, this.length - (to - from) + text.length);
          }
          return super.replace(pos, end, updated);
        }
        pos = end + 1;
      }
    return super.replace(from, to, text);
  }
  sliceString(from, to = this.length, lineSep = "\n") {
    [from, to] = clip(this, from, to);
    let result = "";
    for (let i = 0, pos = 0; i < this.children.length && pos <= to; i++) {
      let child = this.children[i], end = pos + child.length;
      if (pos > from && i)
        result += lineSep;
      if (from < end && to > pos)
        result += child.sliceString(from - pos, to - pos, lineSep);
      pos = end + 1;
    }
    return result;
  }
  flatten(target) {
    for (let child of this.children)
      child.flatten(target);
  }
  scanIdentical(other, dir) {
    if (!(other instanceof _TextNode))
      return 0;
    let length = 0;
    let [iA, iB, eA, eB] = dir > 0 ? [0, 0, this.children.length, other.children.length] : [this.children.length - 1, other.children.length - 1, -1, -1];
    for (; ; iA += dir, iB += dir) {
      if (iA == eA || iB == eB)
        return length;
      let chA = this.children[iA], chB = other.children[iB];
      if (chA != chB)
        return length + chA.scanIdentical(chB, dir);
      length += chA.length + 1;
    }
  }
  static from(children, length = children.reduce((l, ch) => l + ch.length + 1, -1)) {
    let lines = 0;
    for (let ch of children)
      lines += ch.lines;
    if (lines < 32) {
      let flat = [];
      for (let ch of children)
        ch.flatten(flat);
      return new TextLeaf(flat, length);
    }
    let chunk = Math.max(
      32,
      lines >> 5
      /* Tree.BranchShift */
    ), maxChunk = chunk << 1, minChunk = chunk >> 1;
    let chunked = [], currentLines = 0, currentLen = -1, currentChunk = [];
    function add(child) {
      let last;
      if (child.lines > maxChunk && child instanceof _TextNode) {
        for (let node of child.children)
          add(node);
      } else if (child.lines > minChunk && (currentLines > minChunk || !currentLines)) {
        flush();
        chunked.push(child);
      } else if (child instanceof TextLeaf && currentLines && (last = currentChunk[currentChunk.length - 1]) instanceof TextLeaf && child.lines + last.lines <= 32) {
        currentLines += child.lines;
        currentLen += child.length + 1;
        currentChunk[currentChunk.length - 1] = new TextLeaf(last.text.concat(child.text), last.length + 1 + child.length);
      } else {
        if (currentLines + child.lines > chunk)
          flush();
        currentLines += child.lines;
        currentLen += child.length + 1;
        currentChunk.push(child);
      }
    }
    function flush() {
      if (currentLines == 0)
        return;
      chunked.push(currentChunk.length == 1 ? currentChunk[0] : _TextNode.from(currentChunk, currentLen));
      currentLen = -1;
      currentLines = currentChunk.length = 0;
    }
    for (let child of children)
      add(child);
    flush();
    return chunked.length == 1 ? chunked[0] : new _TextNode(chunked, length);
  }
};
Text.empty = /* @__PURE__ */ new TextLeaf([""], 0);
function textLength(text) {
  let length = -1;
  for (let line of text)
    length += line.length + 1;
  return length;
}
function appendText(text, target, from = 0, to = 1e9) {
  for (let pos = 0, i = 0, first = true; i < text.length && pos <= to; i++) {
    let line = text[i], end = pos + line.length;
    if (end >= from) {
      if (end > to)
        line = line.slice(0, to - pos);
      if (pos < from)
        line = line.slice(from - pos);
      if (first) {
        target[target.length - 1] += line;
        first = false;
      } else
        target.push(line);
    }
    pos = end + 1;
  }
  return target;
}
function sliceText(text, from, to) {
  return appendText(text, [""], from, to);
}
var RawTextCursor = class {
  constructor(text, dir = 1) {
    this.dir = dir;
    this.done = false;
    this.lineBreak = false;
    this.value = "";
    this.nodes = [text];
    this.offsets = [dir > 0 ? 1 : (text instanceof TextLeaf ? text.text.length : text.children.length) << 1];
  }
  nextInner(skip, dir) {
    this.done = this.lineBreak = false;
    for (; ; ) {
      let last = this.nodes.length - 1;
      let top2 = this.nodes[last], offsetValue = this.offsets[last], offset = offsetValue >> 1;
      let size = top2 instanceof TextLeaf ? top2.text.length : top2.children.length;
      if (offset == (dir > 0 ? size : 0)) {
        if (last == 0) {
          this.done = true;
          this.value = "";
          return this;
        }
        if (dir > 0)
          this.offsets[last - 1]++;
        this.nodes.pop();
        this.offsets.pop();
      } else if ((offsetValue & 1) == (dir > 0 ? 0 : 1)) {
        this.offsets[last] += dir;
        if (skip == 0) {
          this.lineBreak = true;
          this.value = "\n";
          return this;
        }
        skip--;
      } else if (top2 instanceof TextLeaf) {
        let next = top2.text[offset + (dir < 0 ? -1 : 0)];
        this.offsets[last] += dir;
        if (next.length > Math.max(0, skip)) {
          this.value = skip == 0 ? next : dir > 0 ? next.slice(skip) : next.slice(0, next.length - skip);
          return this;
        }
        skip -= next.length;
      } else {
        let next = top2.children[offset + (dir < 0 ? -1 : 0)];
        if (skip > next.length) {
          skip -= next.length;
          this.offsets[last] += dir;
        } else {
          if (dir < 0)
            this.offsets[last]--;
          this.nodes.push(next);
          this.offsets.push(dir > 0 ? 1 : (next instanceof TextLeaf ? next.text.length : next.children.length) << 1);
        }
      }
    }
  }
  next(skip = 0) {
    if (skip < 0) {
      this.nextInner(-skip, -this.dir);
      skip = this.value.length;
    }
    return this.nextInner(skip, this.dir);
  }
};
var PartialTextCursor = class {
  constructor(text, start, end) {
    this.value = "";
    this.done = false;
    this.cursor = new RawTextCursor(text, start > end ? -1 : 1);
    this.pos = start > end ? text.length : 0;
    this.from = Math.min(start, end);
    this.to = Math.max(start, end);
  }
  nextInner(skip, dir) {
    if (dir < 0 ? this.pos <= this.from : this.pos >= this.to) {
      this.value = "";
      this.done = true;
      return this;
    }
    skip += Math.max(0, dir < 0 ? this.pos - this.to : this.from - this.pos);
    let limit = dir < 0 ? this.pos - this.from : this.to - this.pos;
    if (skip > limit)
      skip = limit;
    limit -= skip;
    let { value } = this.cursor.next(skip);
    this.pos += (value.length + skip) * dir;
    this.value = value.length <= limit ? value : dir < 0 ? value.slice(value.length - limit) : value.slice(0, limit);
    this.done = !this.value;
    return this;
  }
  next(skip = 0) {
    if (skip < 0)
      skip = Math.max(skip, this.from - this.pos);
    else if (skip > 0)
      skip = Math.min(skip, this.to - this.pos);
    return this.nextInner(skip, this.cursor.dir);
  }
  get lineBreak() {
    return this.cursor.lineBreak && this.value != "";
  }
};
var LineCursor = class {
  constructor(inner) {
    this.inner = inner;
    this.afterBreak = true;
    this.value = "";
    this.done = false;
  }
  next(skip = 0) {
    let { done, lineBreak, value } = this.inner.next(skip);
    if (done && this.afterBreak) {
      this.value = "";
      this.afterBreak = false;
    } else if (done) {
      this.done = true;
      this.value = "";
    } else if (lineBreak) {
      if (this.afterBreak) {
        this.value = "";
      } else {
        this.afterBreak = true;
        this.next();
      }
    } else {
      this.value = value;
      this.afterBreak = false;
    }
    return this;
  }
  get lineBreak() {
    return false;
  }
};
if (typeof Symbol != "undefined") {
  Text.prototype[Symbol.iterator] = function() {
    return this.iter();
  };
  RawTextCursor.prototype[Symbol.iterator] = PartialTextCursor.prototype[Symbol.iterator] = LineCursor.prototype[Symbol.iterator] = function() {
    return this;
  };
}
var Line = class {
  /**
  @internal
  */
  constructor(from, to, number2, text) {
    this.from = from;
    this.to = to;
    this.number = number2;
    this.text = text;
  }
  /**
  The length of the line (not including any line break after it).
  */
  get length() {
    return this.to - this.from;
  }
};
function clip(text, from, to) {
  from = Math.max(0, Math.min(text.length, from));
  return [from, Math.max(from, Math.min(text.length, to))];
}
function findClusterBreak2(str, pos, forward = true, includeExtending = true) {
  return findClusterBreak(str, pos, forward, includeExtending);
}
var DefaultSplit = /\r\n?|\n/;
var MapMode = /* @__PURE__ */ (function(MapMode2) {
  MapMode2[MapMode2["Simple"] = 0] = "Simple";
  MapMode2[MapMode2["TrackDel"] = 1] = "TrackDel";
  MapMode2[MapMode2["TrackBefore"] = 2] = "TrackBefore";
  MapMode2[MapMode2["TrackAfter"] = 3] = "TrackAfter";
  return MapMode2;
})(MapMode || (MapMode = {}));
var ChangeDesc = class _ChangeDesc {
  // Sections are encoded as pairs of integers. The first is the
  // length in the current document, and the second is -1 for
  // unaffected sections, and the length of the replacement content
  // otherwise. So an insertion would be (0, n>0), a deletion (n>0,
  // 0), and a replacement two positive numbers.
  /**
  @internal
  */
  constructor(sections) {
    this.sections = sections;
  }
  /**
  The length of the document before the change.
  */
  get length() {
    let result = 0;
    for (let i = 0; i < this.sections.length; i += 2)
      result += this.sections[i];
    return result;
  }
  /**
  The length of the document after the change.
  */
  get newLength() {
    let result = 0;
    for (let i = 0; i < this.sections.length; i += 2) {
      let ins = this.sections[i + 1];
      result += ins < 0 ? this.sections[i] : ins;
    }
    return result;
  }
  /**
  False when there are actual changes in this set.
  */
  get empty() {
    return this.sections.length == 0 || this.sections.length == 2 && this.sections[1] < 0;
  }
  /**
  Iterate over the unchanged parts left by these changes. `posA`
  provides the position of the range in the old document, `posB`
  the new position in the changed document.
  */
  iterGaps(f) {
    for (let i = 0, posA = 0, posB = 0; i < this.sections.length; ) {
      let len = this.sections[i++], ins = this.sections[i++];
      if (ins < 0) {
        f(posA, posB, len);
        posB += len;
      } else {
        posB += ins;
      }
      posA += len;
    }
  }
  /**
  Iterate over the ranges changed by these changes. (See
  [`ChangeSet.iterChanges`](https://codemirror.net/6/docs/ref/#state.ChangeSet.iterChanges) for a
  variant that also provides you with the inserted text.)
  `fromA`/`toA` provides the extent of the change in the starting
  document, `fromB`/`toB` the extent of the replacement in the
  changed document.
  
  When `individual` is true, adjacent changes (which are kept
  separate for [position mapping](https://codemirror.net/6/docs/ref/#state.ChangeDesc.mapPos)) are
  reported separately.
  */
  iterChangedRanges(f, individual = false) {
    iterChanges(this, f, individual);
  }
  /**
  Get a description of the inverted form of these changes.
  */
  get invertedDesc() {
    let sections = [];
    for (let i = 0; i < this.sections.length; ) {
      let len = this.sections[i++], ins = this.sections[i++];
      if (ins < 0)
        sections.push(len, ins);
      else
        sections.push(ins, len);
    }
    return new _ChangeDesc(sections);
  }
  /**
  Compute the combined effect of applying another set of changes
  after this one. The length of the document after this set should
  match the length before `other`.
  */
  composeDesc(other) {
    return this.empty ? other : other.empty ? this : composeSets(this, other);
  }
  /**
  Map this description, which should start with the same document
  as `other`, over another set of changes, so that it can be
  applied after it. When `before` is true, map as if the changes
  in `this` happened before the ones in `other`.
  */
  mapDesc(other, before = false) {
    return other.empty ? this : mapSet(this, other, before);
  }
  mapPos(pos, assoc = -1, mode = MapMode.Simple) {
    let posA = 0, posB = 0;
    for (let i = 0; i < this.sections.length; ) {
      let len = this.sections[i++], ins = this.sections[i++], endA = posA + len;
      if (ins < 0) {
        if (endA > pos)
          return posB + (pos - posA);
        posB += len;
      } else {
        if (mode != MapMode.Simple && endA >= pos && (mode == MapMode.TrackDel && posA < pos && endA > pos || mode == MapMode.TrackBefore && posA < pos || mode == MapMode.TrackAfter && endA > pos))
          return null;
        if (endA > pos || endA == pos && assoc < 0 && !len)
          return pos == posA || assoc < 0 ? posB : posB + ins;
        posB += ins;
      }
      posA = endA;
    }
    if (pos > posA)
      throw new RangeError(`Position ${pos} is out of range for changeset of length ${posA}`);
    return posB;
  }
  /**
  Check whether these changes touch a given range. When one of the
  changes entirely covers the range, the string `"cover"` is
  returned.
  */
  touchesRange(from, to = from) {
    for (let i = 0, pos = 0; i < this.sections.length && pos <= to; ) {
      let len = this.sections[i++], ins = this.sections[i++], end = pos + len;
      if (ins >= 0 && pos <= to && end >= from)
        return pos < from && end > to ? "cover" : true;
      pos = end;
    }
    return false;
  }
  /**
  @internal
  */
  toString() {
    let result = "";
    for (let i = 0; i < this.sections.length; ) {
      let len = this.sections[i++], ins = this.sections[i++];
      result += (result ? " " : "") + len + (ins >= 0 ? ":" + ins : "");
    }
    return result;
  }
  /**
  Serialize this change desc to a JSON-representable value.
  */
  toJSON() {
    return this.sections;
  }
  /**
  Create a change desc from its JSON representation (as produced
  by [`toJSON`](https://codemirror.net/6/docs/ref/#state.ChangeDesc.toJSON).
  */
  static fromJSON(json) {
    if (!Array.isArray(json) || json.length % 2 || json.some((a2) => typeof a2 != "number"))
      throw new RangeError("Invalid JSON representation of ChangeDesc");
    return new _ChangeDesc(json);
  }
  /**
  @internal
  */
  static create(sections) {
    return new _ChangeDesc(sections);
  }
};
var ChangeSet = class _ChangeSet extends ChangeDesc {
  constructor(sections, inserted) {
    super(sections);
    this.inserted = inserted;
  }
  /**
  Apply the changes to a document, returning the modified
  document.
  */
  apply(doc2) {
    if (this.length != doc2.length)
      throw new RangeError("Applying change set to a document with the wrong length");
    iterChanges(this, (fromA, toA, fromB, _toB, text) => doc2 = doc2.replace(fromB, fromB + (toA - fromA), text), false);
    return doc2;
  }
  mapDesc(other, before = false) {
    return mapSet(this, other, before, true);
  }
  /**
  Given the document as it existed _before_ the changes, return a
  change set that represents the inverse of this set, which could
  be used to go from the document created by the changes back to
  the document as it existed before the changes.
  */
  invert(doc2) {
    let sections = this.sections.slice(), inserted = [];
    for (let i = 0, pos = 0; i < sections.length; i += 2) {
      let len = sections[i], ins = sections[i + 1];
      if (ins >= 0) {
        sections[i] = ins;
        sections[i + 1] = len;
        let index = i >> 1;
        while (inserted.length < index)
          inserted.push(Text.empty);
        inserted.push(len ? doc2.slice(pos, pos + len) : Text.empty);
      }
      pos += len;
    }
    return new _ChangeSet(sections, inserted);
  }
  /**
  Combine two subsequent change sets into a single set. `other`
  must start in the document produced by `this`. If `this` goes
  `docA` → `docB` and `other` represents `docB` → `docC`, the
  returned value will represent the change `docA` → `docC`.
  */
  compose(other) {
    return this.empty ? other : other.empty ? this : composeSets(this, other, true);
  }
  /**
  Given another change set starting in the same document, maps this
  change set over the other, producing a new change set that can be
  applied to the document produced by applying `other`. When
  `before` is `true`, order changes as if `this` comes before
  `other`, otherwise (the default) treat `other` as coming first.
  
  Given two changes `A` and `B`, `A.compose(B.map(A))` and
  `B.compose(A.map(B, true))` will produce the same document. This
  provides a basic form of [operational
  transformation](https://en.wikipedia.org/wiki/Operational_transformation),
  and can be used for collaborative editing.
  */
  map(other, before = false) {
    return other.empty ? this : mapSet(this, other, before, true);
  }
  /**
  Iterate over the changed ranges in the document, calling `f` for
  each, with the range in the original document (`fromA`-`toA`)
  and the range that replaces it in the new document
  (`fromB`-`toB`).
  
  When `individual` is true, adjacent changes are reported
  separately.
  */
  iterChanges(f, individual = false) {
    iterChanges(this, f, individual);
  }
  /**
  Get a [change description](https://codemirror.net/6/docs/ref/#state.ChangeDesc) for this change
  set.
  */
  get desc() {
    return ChangeDesc.create(this.sections);
  }
  /**
  @internal
  */
  filter(ranges) {
    let resultSections = [], resultInserted = [], filteredSections = [];
    let iter = new SectionIter(this);
    done: for (let i = 0, pos = 0; ; ) {
      let next = i == ranges.length ? 1e9 : ranges[i++];
      while (pos < next || pos == next && iter.len == 0) {
        if (iter.done)
          break done;
        let len = Math.min(iter.len, next - pos);
        addSection(filteredSections, len, -1);
        let ins = iter.ins == -1 ? -1 : iter.off == 0 ? iter.ins : 0;
        addSection(resultSections, len, ins);
        if (ins > 0)
          addInsert(resultInserted, resultSections, iter.text);
        iter.forward(len);
        pos += len;
      }
      let end = ranges[i++];
      while (pos < end) {
        if (iter.done)
          break done;
        let len = Math.min(iter.len, end - pos);
        addSection(resultSections, len, -1);
        addSection(filteredSections, len, iter.ins == -1 ? -1 : iter.off == 0 ? iter.ins : 0);
        iter.forward(len);
        pos += len;
      }
    }
    return {
      changes: new _ChangeSet(resultSections, resultInserted),
      filtered: ChangeDesc.create(filteredSections)
    };
  }
  /**
  Serialize this change set to a JSON-representable value.
  */
  toJSON() {
    let parts = [];
    for (let i = 0; i < this.sections.length; i += 2) {
      let len = this.sections[i], ins = this.sections[i + 1];
      if (ins < 0)
        parts.push(len);
      else if (ins == 0)
        parts.push([len]);
      else
        parts.push([len].concat(this.inserted[i >> 1].toJSON()));
    }
    return parts;
  }
  /**
  Create a change set for the given changes, for a document of the
  given length, using `lineSep` as line separator.
  */
  static of(changes, length, lineSep) {
    let sections = [], inserted = [], pos = 0;
    let total = null;
    function flush(force = false) {
      if (!force && !sections.length)
        return;
      if (pos < length)
        addSection(sections, length - pos, -1);
      let set = new _ChangeSet(sections, inserted);
      total = total ? total.compose(set.map(total)) : set;
      sections = [];
      inserted = [];
      pos = 0;
    }
    function process2(spec) {
      if (Array.isArray(spec)) {
        for (let sub of spec)
          process2(sub);
      } else if (spec instanceof _ChangeSet) {
        if (spec.length != length)
          throw new RangeError(`Mismatched change set length (got ${spec.length}, expected ${length})`);
        flush();
        total = total ? total.compose(spec.map(total)) : spec;
      } else {
        let { from, to = from, insert: insert2 } = spec;
        if (from > to || from < 0 || to > length)
          throw new RangeError(`Invalid change range ${from} to ${to} (in doc of length ${length})`);
        let insText = !insert2 ? Text.empty : typeof insert2 == "string" ? Text.of(insert2.split(lineSep || DefaultSplit)) : insert2;
        let insLen = insText.length;
        if (from == to && insLen == 0)
          return;
        if (from < pos)
          flush();
        if (from > pos)
          addSection(sections, from - pos, -1);
        addSection(sections, to - from, insLen);
        addInsert(inserted, sections, insText);
        pos = to;
      }
    }
    process2(changes);
    flush(!total);
    return total;
  }
  /**
  Create an empty changeset of the given length.
  */
  static empty(length) {
    return new _ChangeSet(length ? [length, -1] : [], []);
  }
  /**
  Create a changeset from its JSON representation (as produced by
  [`toJSON`](https://codemirror.net/6/docs/ref/#state.ChangeSet.toJSON).
  */
  static fromJSON(json) {
    if (!Array.isArray(json))
      throw new RangeError("Invalid JSON representation of ChangeSet");
    let sections = [], inserted = [];
    for (let i = 0; i < json.length; i++) {
      let part = json[i];
      if (typeof part == "number") {
        sections.push(part, -1);
      } else if (!Array.isArray(part) || typeof part[0] != "number" || part.some((e, i2) => i2 && typeof e != "string")) {
        throw new RangeError("Invalid JSON representation of ChangeSet");
      } else if (part.length == 1) {
        sections.push(part[0], 0);
      } else {
        while (inserted.length < i)
          inserted.push(Text.empty);
        inserted[i] = Text.of(part.slice(1));
        sections.push(part[0], inserted[i].length);
      }
    }
    return new _ChangeSet(sections, inserted);
  }
  /**
  @internal
  */
  static createSet(sections, inserted) {
    return new _ChangeSet(sections, inserted);
  }
};
function addSection(sections, len, ins, forceJoin = false) {
  if (len == 0 && ins <= 0)
    return;
  let last = sections.length - 2;
  if (last >= 0 && ins <= 0 && ins == sections[last + 1])
    sections[last] += len;
  else if (last >= 0 && len == 0 && sections[last] == 0)
    sections[last + 1] += ins;
  else if (forceJoin) {
    sections[last] += len;
    sections[last + 1] += ins;
  } else
    sections.push(len, ins);
}
function addInsert(values, sections, value) {
  if (value.length == 0)
    return;
  let index = sections.length - 2 >> 1;
  if (index < values.length) {
    values[values.length - 1] = values[values.length - 1].append(value);
  } else {
    while (values.length < index)
      values.push(Text.empty);
    values.push(value);
  }
}
function iterChanges(desc, f, individual) {
  let inserted = desc.inserted;
  for (let posA = 0, posB = 0, i = 0; i < desc.sections.length; ) {
    let len = desc.sections[i++], ins = desc.sections[i++];
    if (ins < 0) {
      posA += len;
      posB += len;
    } else {
      let endA = posA, endB = posB, text = Text.empty;
      for (; ; ) {
        endA += len;
        endB += ins;
        if (ins && inserted)
          text = text.append(inserted[i - 2 >> 1]);
        if (individual || i == desc.sections.length || desc.sections[i + 1] < 0)
          break;
        len = desc.sections[i++];
        ins = desc.sections[i++];
      }
      f(posA, endA, posB, endB, text);
      posA = endA;
      posB = endB;
    }
  }
}
function mapSet(setA, setB, before, mkSet = false) {
  let sections = [], insert2 = mkSet ? [] : null;
  let a2 = new SectionIter(setA), b = new SectionIter(setB);
  for (let inserted = -1; ; ) {
    if (a2.done && b.len || b.done && a2.len) {
      throw new Error("Mismatched change set lengths");
    } else if (a2.ins == -1 && b.ins == -1) {
      let len = Math.min(a2.len, b.len);
      addSection(sections, len, -1);
      a2.forward(len);
      b.forward(len);
    } else if (b.ins >= 0 && (a2.ins < 0 || inserted == a2.i || a2.off == 0 && (b.len < a2.len || b.len == a2.len && !before))) {
      let len = b.len;
      addSection(sections, b.ins, -1);
      while (len) {
        let piece = Math.min(a2.len, len);
        if (a2.ins >= 0 && inserted < a2.i && a2.len <= piece) {
          addSection(sections, 0, a2.ins);
          if (insert2)
            addInsert(insert2, sections, a2.text);
          inserted = a2.i;
        }
        a2.forward(piece);
        len -= piece;
      }
      b.next();
    } else if (a2.ins >= 0) {
      let len = 0, left = a2.len;
      while (left) {
        if (b.ins == -1) {
          let piece = Math.min(left, b.len);
          len += piece;
          left -= piece;
          b.forward(piece);
        } else if (b.ins == 0 && b.len < left) {
          left -= b.len;
          b.next();
        } else {
          break;
        }
      }
      addSection(sections, len, inserted < a2.i ? a2.ins : 0);
      if (insert2 && inserted < a2.i)
        addInsert(insert2, sections, a2.text);
      inserted = a2.i;
      a2.forward(a2.len - left);
    } else if (a2.done && b.done) {
      return insert2 ? ChangeSet.createSet(sections, insert2) : ChangeDesc.create(sections);
    } else {
      throw new Error("Mismatched change set lengths");
    }
  }
}
function composeSets(setA, setB, mkSet = false) {
  let sections = [];
  let insert2 = mkSet ? [] : null;
  let a2 = new SectionIter(setA), b = new SectionIter(setB);
  for (let open = false; ; ) {
    if (a2.done && b.done) {
      return insert2 ? ChangeSet.createSet(sections, insert2) : ChangeDesc.create(sections);
    } else if (a2.ins == 0) {
      addSection(sections, a2.len, 0, open);
      a2.next();
    } else if (b.len == 0 && !b.done) {
      addSection(sections, 0, b.ins, open);
      if (insert2)
        addInsert(insert2, sections, b.text);
      b.next();
    } else if (a2.done || b.done) {
      throw new Error("Mismatched change set lengths");
    } else {
      let len = Math.min(a2.len2, b.len), sectionLen = sections.length;
      if (a2.ins == -1) {
        let insB = b.ins == -1 ? -1 : b.off ? 0 : b.ins;
        addSection(sections, len, insB, open);
        if (insert2 && insB)
          addInsert(insert2, sections, b.text);
      } else if (b.ins == -1) {
        addSection(sections, a2.off ? 0 : a2.len, len, open);
        if (insert2)
          addInsert(insert2, sections, a2.textBit(len));
      } else {
        addSection(sections, a2.off ? 0 : a2.len, b.off ? 0 : b.ins, open);
        if (insert2 && !b.off)
          addInsert(insert2, sections, b.text);
      }
      open = (a2.ins > len || b.ins >= 0 && b.len > len) && (open || sections.length > sectionLen);
      a2.forward2(len);
      b.forward(len);
    }
  }
}
var SectionIter = class {
  constructor(set) {
    this.set = set;
    this.i = 0;
    this.next();
  }
  next() {
    let { sections } = this.set;
    if (this.i < sections.length) {
      this.len = sections[this.i++];
      this.ins = sections[this.i++];
    } else {
      this.len = 0;
      this.ins = -2;
    }
    this.off = 0;
  }
  get done() {
    return this.ins == -2;
  }
  get len2() {
    return this.ins < 0 ? this.len : this.ins;
  }
  get text() {
    let { inserted } = this.set, index = this.i - 2 >> 1;
    return index >= inserted.length ? Text.empty : inserted[index];
  }
  textBit(len) {
    let { inserted } = this.set, index = this.i - 2 >> 1;
    return index >= inserted.length && !len ? Text.empty : inserted[index].slice(this.off, len == null ? void 0 : this.off + len);
  }
  forward(len) {
    if (len == this.len)
      this.next();
    else {
      this.len -= len;
      this.off += len;
    }
  }
  forward2(len) {
    if (this.ins == -1)
      this.forward(len);
    else if (len == this.ins)
      this.next();
    else {
      this.ins -= len;
      this.off += len;
    }
  }
};
var SelectionRange = class _SelectionRange {
  constructor(from, to, flags) {
    this.from = from;
    this.to = to;
    this.flags = flags;
  }
  /**
  The anchor of the range—the side that doesn't move when you
  extend it.
  */
  get anchor() {
    return this.flags & 32 ? this.to : this.from;
  }
  /**
  The head of the range, which is moved when the range is
  [extended](https://codemirror.net/6/docs/ref/#state.SelectionRange.extend).
  */
  get head() {
    return this.flags & 32 ? this.from : this.to;
  }
  /**
  True when `anchor` and `head` are at the same position.
  */
  get empty() {
    return this.from == this.to;
  }
  /**
  If this is a cursor that is explicitly associated with the
  character on one of its sides, this returns the side. -1 means
  the character before its position, 1 the character after, and 0
  means no association.
  */
  get assoc() {
    return this.flags & 8 ? -1 : this.flags & 16 ? 1 : 0;
  }
  /**
  The bidirectional text level associated with this cursor, if
  any.
  */
  get bidiLevel() {
    let level = this.flags & 7;
    return level == 7 ? null : level;
  }
  /**
  The goal column (stored vertical offset) associated with a
  cursor. This is used to preserve the vertical position when
  [moving](https://codemirror.net/6/docs/ref/#view.EditorView.moveVertically) across
  lines of different length.
  */
  get goalColumn() {
    let value = this.flags >> 6;
    return value == 16777215 ? void 0 : value;
  }
  /**
  Map this range through a change, producing a valid range in the
  updated document.
  */
  map(change, assoc = -1) {
    let from, to;
    if (this.empty) {
      from = to = change.mapPos(this.from, assoc);
    } else {
      from = change.mapPos(this.from, 1);
      to = change.mapPos(this.to, -1);
    }
    return from == this.from && to == this.to ? this : new _SelectionRange(from, to, this.flags);
  }
  /**
  Extend this range to cover at least `from` to `to`.
  */
  extend(from, to = from) {
    if (from <= this.anchor && to >= this.anchor)
      return EditorSelection.range(from, to);
    let head = Math.abs(from - this.anchor) > Math.abs(to - this.anchor) ? from : to;
    return EditorSelection.range(this.anchor, head);
  }
  /**
  Compare this range to another range.
  */
  eq(other, includeAssoc = false) {
    return this.anchor == other.anchor && this.head == other.head && (!includeAssoc || !this.empty || this.assoc == other.assoc);
  }
  /**
  Return a JSON-serializable object representing the range.
  */
  toJSON() {
    return { anchor: this.anchor, head: this.head };
  }
  /**
  Convert a JSON representation of a range to a `SelectionRange`
  instance.
  */
  static fromJSON(json) {
    if (!json || typeof json.anchor != "number" || typeof json.head != "number")
      throw new RangeError("Invalid JSON representation for SelectionRange");
    return EditorSelection.range(json.anchor, json.head);
  }
  /**
  @internal
  */
  static create(from, to, flags) {
    return new _SelectionRange(from, to, flags);
  }
};
var EditorSelection = class _EditorSelection {
  constructor(ranges, mainIndex) {
    this.ranges = ranges;
    this.mainIndex = mainIndex;
  }
  /**
  Map a selection through a change. Used to adjust the selection
  position for changes.
  */
  map(change, assoc = -1) {
    if (change.empty)
      return this;
    return _EditorSelection.create(this.ranges.map((r) => r.map(change, assoc)), this.mainIndex);
  }
  /**
  Compare this selection to another selection. By default, ranges
  are compared only by position. When `includeAssoc` is true,
  cursor ranges must also have the same
  [`assoc`](https://codemirror.net/6/docs/ref/#state.SelectionRange.assoc) value.
  */
  eq(other, includeAssoc = false) {
    if (this.ranges.length != other.ranges.length || this.mainIndex != other.mainIndex)
      return false;
    for (let i = 0; i < this.ranges.length; i++)
      if (!this.ranges[i].eq(other.ranges[i], includeAssoc))
        return false;
    return true;
  }
  /**
  Get the primary selection range. Usually, you should make sure
  your code applies to _all_ ranges, by using methods like
  [`changeByRange`](https://codemirror.net/6/docs/ref/#state.EditorState.changeByRange).
  */
  get main() {
    return this.ranges[this.mainIndex];
  }
  /**
  Make sure the selection only has one range. Returns a selection
  holding only the main range from this selection.
  */
  asSingle() {
    return this.ranges.length == 1 ? this : new _EditorSelection([this.main], 0);
  }
  /**
  Extend this selection with an extra range.
  */
  addRange(range, main = true) {
    return _EditorSelection.create([range].concat(this.ranges), main ? 0 : this.mainIndex + 1);
  }
  /**
  Replace a given range with another range, and then normalize the
  selection to merge and sort ranges if necessary.
  */
  replaceRange(range, which = this.mainIndex) {
    let ranges = this.ranges.slice();
    ranges[which] = range;
    return _EditorSelection.create(ranges, this.mainIndex);
  }
  /**
  Convert this selection to an object that can be serialized to
  JSON.
  */
  toJSON() {
    return { ranges: this.ranges.map((r) => r.toJSON()), main: this.mainIndex };
  }
  /**
  Create a selection from a JSON representation.
  */
  static fromJSON(json) {
    if (!json || !Array.isArray(json.ranges) || typeof json.main != "number" || json.main >= json.ranges.length)
      throw new RangeError("Invalid JSON representation for EditorSelection");
    return new _EditorSelection(json.ranges.map((r) => SelectionRange.fromJSON(r)), json.main);
  }
  /**
  Create a selection holding a single range.
  */
  static single(anchor, head = anchor) {
    return new _EditorSelection([_EditorSelection.range(anchor, head)], 0);
  }
  /**
  Sort and merge the given set of ranges, creating a valid
  selection.
  */
  static create(ranges, mainIndex = 0) {
    if (ranges.length == 0)
      throw new RangeError("A selection needs at least one range");
    for (let pos = 0, i = 0; i < ranges.length; i++) {
      let range = ranges[i];
      if (range.empty ? range.from <= pos : range.from < pos)
        return _EditorSelection.normalized(ranges.slice(), mainIndex);
      pos = range.to;
    }
    return new _EditorSelection(ranges, mainIndex);
  }
  /**
  Create a cursor selection range at the given position. You can
  safely ignore the optional arguments in most situations.
  */
  static cursor(pos, assoc = 0, bidiLevel, goalColumn) {
    return SelectionRange.create(pos, pos, (assoc == 0 ? 0 : assoc < 0 ? 8 : 16) | (bidiLevel == null ? 7 : Math.min(6, bidiLevel)) | (goalColumn !== null && goalColumn !== void 0 ? goalColumn : 16777215) << 6);
  }
  /**
  Create a selection range.
  */
  static range(anchor, head, goalColumn, bidiLevel) {
    let flags = (goalColumn !== null && goalColumn !== void 0 ? goalColumn : 16777215) << 6 | (bidiLevel == null ? 7 : Math.min(6, bidiLevel));
    return head < anchor ? SelectionRange.create(head, anchor, 32 | 16 | flags) : SelectionRange.create(anchor, head, (head > anchor ? 8 : 0) | flags);
  }
  /**
  @internal
  */
  static normalized(ranges, mainIndex = 0) {
    let main = ranges[mainIndex];
    ranges.sort((a2, b) => a2.from - b.from);
    mainIndex = ranges.indexOf(main);
    for (let i = 1; i < ranges.length; i++) {
      let range = ranges[i], prev = ranges[i - 1];
      if (range.empty ? range.from <= prev.to : range.from < prev.to) {
        let from = prev.from, to = Math.max(range.to, prev.to);
        if (i <= mainIndex)
          mainIndex--;
        ranges.splice(--i, 2, range.anchor > range.head ? _EditorSelection.range(to, from) : _EditorSelection.range(from, to));
      }
    }
    return new _EditorSelection(ranges, mainIndex);
  }
};
function checkSelection(selection, docLength) {
  for (let range of selection.ranges)
    if (range.to > docLength)
      throw new RangeError("Selection points outside of document");
}
var nextID = 0;
var Facet = class _Facet {
  constructor(combine, compareInput, compare2, isStatic, enables) {
    this.combine = combine;
    this.compareInput = compareInput;
    this.compare = compare2;
    this.isStatic = isStatic;
    this.id = nextID++;
    this.default = combine([]);
    this.extensions = typeof enables == "function" ? enables(this) : enables;
  }
  /**
  Returns a facet reader for this facet, which can be used to
  [read](https://codemirror.net/6/docs/ref/#state.EditorState.facet) it but not to define values for it.
  */
  get reader() {
    return this;
  }
  /**
  Define a new facet.
  */
  static define(config = {}) {
    return new _Facet(config.combine || ((a2) => a2), config.compareInput || ((a2, b) => a2 === b), config.compare || (!config.combine ? sameArray2 : (a2, b) => a2 === b), !!config.static, config.enables);
  }
  /**
  Returns an extension that adds the given value to this facet.
  */
  of(value) {
    return new FacetProvider([], this, 0, value);
  }
  /**
  Create an extension that computes a value for the facet from a
  state. You must take care to declare the parts of the state that
  this value depends on, since your function is only called again
  for a new state when one of those parts changed.
  
  In cases where your value depends only on a single field, you'll
  want to use the [`from`](https://codemirror.net/6/docs/ref/#state.Facet.from) method instead.
  */
  compute(deps, get) {
    if (this.isStatic)
      throw new Error("Can't compute a static facet");
    return new FacetProvider(deps, this, 1, get);
  }
  /**
  Create an extension that computes zero or more values for this
  facet from a state.
  */
  computeN(deps, get) {
    if (this.isStatic)
      throw new Error("Can't compute a static facet");
    return new FacetProvider(deps, this, 2, get);
  }
  from(field, get) {
    if (!get)
      get = (x) => x;
    return this.compute([field], (state) => get(state.field(field)));
  }
};
function sameArray2(a2, b) {
  return a2 == b || a2.length == b.length && a2.every((e, i) => e === b[i]);
}
var FacetProvider = class {
  constructor(dependencies, facet, type, value) {
    this.dependencies = dependencies;
    this.facet = facet;
    this.type = type;
    this.value = value;
    this.id = nextID++;
  }
  dynamicSlot(addresses) {
    var _a2;
    let getter = this.value;
    let compare2 = this.facet.compareInput;
    let id = this.id, idx = addresses[id] >> 1, multi = this.type == 2;
    let depDoc = false, depSel = false, depAddrs = [];
    for (let dep of this.dependencies) {
      if (dep == "doc")
        depDoc = true;
      else if (dep == "selection")
        depSel = true;
      else if ((((_a2 = addresses[dep.id]) !== null && _a2 !== void 0 ? _a2 : 1) & 1) == 0)
        depAddrs.push(addresses[dep.id]);
    }
    return {
      create(state) {
        state.values[idx] = getter(state);
        return 1;
      },
      update(state, tr) {
        if (depDoc && tr.docChanged || depSel && (tr.docChanged || tr.selection) || ensureAll(state, depAddrs)) {
          let newVal = getter(state);
          if (multi ? !compareArray(newVal, state.values[idx], compare2) : !compare2(newVal, state.values[idx])) {
            state.values[idx] = newVal;
            return 1;
          }
        }
        return 0;
      },
      reconfigure: (state, oldState) => {
        let newVal, oldAddr = oldState.config.address[id];
        if (oldAddr != null) {
          let oldVal = getAddr(oldState, oldAddr);
          if (this.dependencies.every((dep) => {
            return dep instanceof Facet ? oldState.facet(dep) === state.facet(dep) : dep instanceof StateField ? oldState.field(dep, false) == state.field(dep, false) : true;
          }) || (multi ? compareArray(newVal = getter(state), oldVal, compare2) : compare2(newVal = getter(state), oldVal))) {
            state.values[idx] = oldVal;
            return 0;
          }
        } else {
          newVal = getter(state);
        }
        state.values[idx] = newVal;
        return 1;
      }
    };
  }
};
function compareArray(a2, b, compare2) {
  if (a2.length != b.length)
    return false;
  for (let i = 0; i < a2.length; i++)
    if (!compare2(a2[i], b[i]))
      return false;
  return true;
}
function ensureAll(state, addrs) {
  let changed = false;
  for (let addr of addrs)
    if (ensureAddr(state, addr) & 1)
      changed = true;
  return changed;
}
function dynamicFacetSlot(addresses, facet, providers) {
  let providerAddrs = providers.map((p) => addresses[p.id]);
  let providerTypes = providers.map((p) => p.type);
  let dynamic = providerAddrs.filter((p) => !(p & 1));
  let idx = addresses[facet.id] >> 1;
  function get(state) {
    let values = [];
    for (let i = 0; i < providerAddrs.length; i++) {
      let value = getAddr(state, providerAddrs[i]);
      if (providerTypes[i] == 2)
        for (let val of value)
          values.push(val);
      else
        values.push(value);
    }
    return facet.combine(values);
  }
  return {
    create(state) {
      for (let addr of providerAddrs)
        ensureAddr(state, addr);
      state.values[idx] = get(state);
      return 1;
    },
    update(state, tr) {
      if (!ensureAll(state, dynamic))
        return 0;
      let value = get(state);
      if (facet.compare(value, state.values[idx]))
        return 0;
      state.values[idx] = value;
      return 1;
    },
    reconfigure(state, oldState) {
      let depChanged = ensureAll(state, providerAddrs);
      let oldProviders = oldState.config.facets[facet.id], oldValue = oldState.facet(facet);
      if (oldProviders && !depChanged && sameArray2(providers, oldProviders)) {
        state.values[idx] = oldValue;
        return 0;
      }
      let value = get(state);
      if (facet.compare(value, oldValue)) {
        state.values[idx] = oldValue;
        return 0;
      }
      state.values[idx] = value;
      return 1;
    }
  };
}
var initField = /* @__PURE__ */ Facet.define({ static: true });
var StateField = class _StateField {
  constructor(id, createF, updateF, compareF, spec) {
    this.id = id;
    this.createF = createF;
    this.updateF = updateF;
    this.compareF = compareF;
    this.spec = spec;
    this.provides = void 0;
  }
  /**
  Define a state field.
  */
  static define(config) {
    let field = new _StateField(nextID++, config.create, config.update, config.compare || ((a2, b) => a2 === b), config);
    if (config.provide)
      field.provides = config.provide(field);
    return field;
  }
  create(state) {
    let init = state.facet(initField).find((i) => i.field == this);
    return ((init === null || init === void 0 ? void 0 : init.create) || this.createF)(state);
  }
  /**
  @internal
  */
  slot(addresses) {
    let idx = addresses[this.id] >> 1;
    return {
      create: (state) => {
        state.values[idx] = this.create(state);
        return 1;
      },
      update: (state, tr) => {
        let oldVal = state.values[idx];
        let value = this.updateF(oldVal, tr);
        if (this.compareF(oldVal, value))
          return 0;
        state.values[idx] = value;
        return 1;
      },
      reconfigure: (state, oldState) => {
        let init = state.facet(initField), oldInit = oldState.facet(initField), reInit;
        if ((reInit = init.find((i) => i.field == this)) && reInit != oldInit.find((i) => i.field == this)) {
          state.values[idx] = reInit.create(state);
          return 1;
        }
        if (oldState.config.address[this.id] != null) {
          state.values[idx] = oldState.field(this);
          return 0;
        }
        state.values[idx] = this.create(state);
        return 1;
      }
    };
  }
  /**
  Returns an extension that enables this field and overrides the
  way it is initialized. Can be useful when you need to provide a
  non-default starting value for the field.
  */
  init(create) {
    return [this, initField.of({ field: this, create })];
  }
  /**
  State field instances can be used as
  [`Extension`](https://codemirror.net/6/docs/ref/#state.Extension) values to enable the field in a
  given state.
  */
  get extension() {
    return this;
  }
};
var Prec_ = { lowest: 4, low: 3, default: 2, high: 1, highest: 0 };
function prec(value) {
  return (ext) => new PrecExtension(ext, value);
}
var Prec = {
  /**
  The highest precedence level, for extensions that should end up
  near the start of the precedence ordering.
  */
  highest: /* @__PURE__ */ prec(Prec_.highest),
  /**
  A higher-than-default precedence, for extensions that should
  come before those with default precedence.
  */
  high: /* @__PURE__ */ prec(Prec_.high),
  /**
  The default precedence, which is also used for extensions
  without an explicit precedence.
  */
  default: /* @__PURE__ */ prec(Prec_.default),
  /**
  A lower-than-default precedence.
  */
  low: /* @__PURE__ */ prec(Prec_.low),
  /**
  The lowest precedence level. Meant for things that should end up
  near the end of the extension order.
  */
  lowest: /* @__PURE__ */ prec(Prec_.lowest)
};
var PrecExtension = class {
  constructor(inner, prec2) {
    this.inner = inner;
    this.prec = prec2;
  }
};
var Compartment = class _Compartment {
  /**
  Create an instance of this compartment to add to your [state
  configuration](https://codemirror.net/6/docs/ref/#state.EditorStateConfig.extensions).
  */
  of(ext) {
    return new CompartmentInstance(this, ext);
  }
  /**
  Create an [effect](https://codemirror.net/6/docs/ref/#state.TransactionSpec.effects) that
  reconfigures this compartment.
  */
  reconfigure(content2) {
    return _Compartment.reconfigure.of({ compartment: this, extension: content2 });
  }
  /**
  Get the current content of the compartment in the state, or
  `undefined` if it isn't present.
  */
  get(state) {
    return state.config.compartments.get(this);
  }
};
var CompartmentInstance = class {
  constructor(compartment, inner) {
    this.compartment = compartment;
    this.inner = inner;
  }
};
var Configuration = class _Configuration {
  constructor(base2, compartments, dynamicSlots, address, staticValues, facets) {
    this.base = base2;
    this.compartments = compartments;
    this.dynamicSlots = dynamicSlots;
    this.address = address;
    this.staticValues = staticValues;
    this.facets = facets;
    this.statusTemplate = [];
    while (this.statusTemplate.length < dynamicSlots.length)
      this.statusTemplate.push(
        0
        /* SlotStatus.Unresolved */
      );
  }
  staticFacet(facet) {
    let addr = this.address[facet.id];
    return addr == null ? facet.default : this.staticValues[addr >> 1];
  }
  static resolve(base2, compartments, oldState) {
    let fields = [];
    let facets = /* @__PURE__ */ Object.create(null);
    let newCompartments = /* @__PURE__ */ new Map();
    for (let ext of flatten(base2, compartments, newCompartments)) {
      if (ext instanceof StateField)
        fields.push(ext);
      else
        (facets[ext.facet.id] || (facets[ext.facet.id] = [])).push(ext);
    }
    let address = /* @__PURE__ */ Object.create(null);
    let staticValues = [];
    let dynamicSlots = [];
    for (let field of fields) {
      address[field.id] = dynamicSlots.length << 1;
      dynamicSlots.push((a2) => field.slot(a2));
    }
    let oldFacets = oldState === null || oldState === void 0 ? void 0 : oldState.config.facets;
    for (let id in facets) {
      let providers = facets[id], facet = providers[0].facet;
      let oldProviders = oldFacets && oldFacets[id] || [];
      if (providers.every(
        (p) => p.type == 0
        /* Provider.Static */
      )) {
        address[facet.id] = staticValues.length << 1 | 1;
        if (sameArray2(oldProviders, providers)) {
          staticValues.push(oldState.facet(facet));
        } else {
          let value = facet.combine(providers.map((p) => p.value));
          staticValues.push(oldState && facet.compare(value, oldState.facet(facet)) ? oldState.facet(facet) : value);
        }
      } else {
        for (let p of providers) {
          if (p.type == 0) {
            address[p.id] = staticValues.length << 1 | 1;
            staticValues.push(p.value);
          } else {
            address[p.id] = dynamicSlots.length << 1;
            dynamicSlots.push((a2) => p.dynamicSlot(a2));
          }
        }
        address[facet.id] = dynamicSlots.length << 1;
        dynamicSlots.push((a2) => dynamicFacetSlot(a2, facet, providers));
      }
    }
    let dynamic = dynamicSlots.map((f) => f(address));
    return new _Configuration(base2, newCompartments, dynamic, address, staticValues, facets);
  }
};
function flatten(extension, compartments, newCompartments) {
  let result = [[], [], [], [], []];
  let seen = /* @__PURE__ */ new Map();
  function inner(ext, prec2) {
    let known = seen.get(ext);
    if (known != null) {
      if (known <= prec2)
        return;
      let found = result[known].indexOf(ext);
      if (found > -1)
        result[known].splice(found, 1);
      if (ext instanceof CompartmentInstance)
        newCompartments.delete(ext.compartment);
    }
    seen.set(ext, prec2);
    if (Array.isArray(ext)) {
      for (let e of ext)
        inner(e, prec2);
    } else if (ext instanceof CompartmentInstance) {
      if (newCompartments.has(ext.compartment))
        throw new RangeError(`Duplicate use of compartment in extensions`);
      let content2 = compartments.get(ext.compartment) || ext.inner;
      newCompartments.set(ext.compartment, content2);
      inner(content2, prec2);
    } else if (ext instanceof PrecExtension) {
      inner(ext.inner, ext.prec);
    } else if (ext instanceof StateField) {
      result[prec2].push(ext);
      if (ext.provides)
        inner(ext.provides, prec2);
    } else if (ext instanceof FacetProvider) {
      result[prec2].push(ext);
      if (ext.facet.extensions)
        inner(ext.facet.extensions, Prec_.default);
    } else {
      let content2 = ext.extension;
      if (!content2)
        throw new Error(`Unrecognized extension value in extension set (${ext}). This sometimes happens because multiple instances of @codemirror/state are loaded, breaking instanceof checks.`);
      inner(content2, prec2);
    }
  }
  inner(extension, Prec_.default);
  return result.reduce((a2, b) => a2.concat(b));
}
function ensureAddr(state, addr) {
  if (addr & 1)
    return 2;
  let idx = addr >> 1;
  let status = state.status[idx];
  if (status == 4)
    throw new Error("Cyclic dependency between fields and/or facets");
  if (status & 2)
    return status;
  state.status[idx] = 4;
  let changed = state.computeSlot(state, state.config.dynamicSlots[idx]);
  return state.status[idx] = 2 | changed;
}
function getAddr(state, addr) {
  return addr & 1 ? state.config.staticValues[addr >> 1] : state.values[addr >> 1];
}
var languageData = /* @__PURE__ */ Facet.define();
var allowMultipleSelections = /* @__PURE__ */ Facet.define({
  combine: (values) => values.some((v) => v),
  static: true
});
var lineSeparator = /* @__PURE__ */ Facet.define({
  combine: (values) => values.length ? values[0] : void 0,
  static: true
});
var changeFilter = /* @__PURE__ */ Facet.define();
var transactionFilter = /* @__PURE__ */ Facet.define();
var transactionExtender = /* @__PURE__ */ Facet.define();
var readOnly = /* @__PURE__ */ Facet.define({
  combine: (values) => values.length ? values[0] : false
});
var Annotation = class {
  /**
  @internal
  */
  constructor(type, value) {
    this.type = type;
    this.value = value;
  }
  /**
  Define a new type of annotation.
  */
  static define() {
    return new AnnotationType();
  }
};
var AnnotationType = class {
  /**
  Create an instance of this annotation.
  */
  of(value) {
    return new Annotation(this, value);
  }
};
var StateEffectType = class {
  /**
  @internal
  */
  constructor(map) {
    this.map = map;
  }
  /**
  Create a [state effect](https://codemirror.net/6/docs/ref/#state.StateEffect) instance of this
  type.
  */
  of(value) {
    return new StateEffect(this, value);
  }
};
var StateEffect = class _StateEffect {
  /**
  @internal
  */
  constructor(type, value) {
    this.type = type;
    this.value = value;
  }
  /**
  Map this effect through a position mapping. Will return
  `undefined` when that ends up deleting the effect.
  */
  map(mapping) {
    let mapped = this.type.map(this.value, mapping);
    return mapped === void 0 ? void 0 : mapped == this.value ? this : new _StateEffect(this.type, mapped);
  }
  /**
  Tells you whether this effect object is of a given
  [type](https://codemirror.net/6/docs/ref/#state.StateEffectType).
  */
  is(type) {
    return this.type == type;
  }
  /**
  Define a new effect type. The type parameter indicates the type
  of values that his effect holds. It should be a type that
  doesn't include `undefined`, since that is used in
  [mapping](https://codemirror.net/6/docs/ref/#state.StateEffect.map) to indicate that an effect is
  removed.
  */
  static define(spec = {}) {
    return new StateEffectType(spec.map || ((v) => v));
  }
  /**
  Map an array of effects through a change set.
  */
  static mapEffects(effects, mapping) {
    if (!effects.length)
      return effects;
    let result = [];
    for (let effect of effects) {
      let mapped = effect.map(mapping);
      if (mapped)
        result.push(mapped);
    }
    return result;
  }
};
StateEffect.reconfigure = /* @__PURE__ */ StateEffect.define();
StateEffect.appendConfig = /* @__PURE__ */ StateEffect.define();
var Transaction = class _Transaction {
  constructor(startState, changes, selection, effects, annotations, scrollIntoView2) {
    this.startState = startState;
    this.changes = changes;
    this.selection = selection;
    this.effects = effects;
    this.annotations = annotations;
    this.scrollIntoView = scrollIntoView2;
    this._doc = null;
    this._state = null;
    if (selection)
      checkSelection(selection, changes.newLength);
    if (!annotations.some((a2) => a2.type == _Transaction.time))
      this.annotations = annotations.concat(_Transaction.time.of(Date.now()));
  }
  /**
  @internal
  */
  static create(startState, changes, selection, effects, annotations, scrollIntoView2) {
    return new _Transaction(startState, changes, selection, effects, annotations, scrollIntoView2);
  }
  /**
  The new document produced by the transaction. Contrary to
  [`.state`](https://codemirror.net/6/docs/ref/#state.Transaction.state)`.doc`, accessing this won't
  force the entire new state to be computed right away, so it is
  recommended that [transaction
  filters](https://codemirror.net/6/docs/ref/#state.EditorState^transactionFilter) use this getter
  when they need to look at the new document.
  */
  get newDoc() {
    return this._doc || (this._doc = this.changes.apply(this.startState.doc));
  }
  /**
  The new selection produced by the transaction. If
  [`this.selection`](https://codemirror.net/6/docs/ref/#state.Transaction.selection) is undefined,
  this will [map](https://codemirror.net/6/docs/ref/#state.EditorSelection.map) the start state's
  current selection through the changes made by the transaction.
  */
  get newSelection() {
    return this.selection || this.startState.selection.map(this.changes);
  }
  /**
  The new state created by the transaction. Computed on demand
  (but retained for subsequent access), so it is recommended not to
  access it in [transaction
  filters](https://codemirror.net/6/docs/ref/#state.EditorState^transactionFilter) when possible.
  */
  get state() {
    if (!this._state)
      this.startState.applyTransaction(this);
    return this._state;
  }
  /**
  Get the value of the given annotation type, if any.
  */
  annotation(type) {
    for (let ann of this.annotations)
      if (ann.type == type)
        return ann.value;
    return void 0;
  }
  /**
  Indicates whether the transaction changed the document.
  */
  get docChanged() {
    return !this.changes.empty;
  }
  /**
  Indicates whether this transaction reconfigures the state
  (through a [configuration compartment](https://codemirror.net/6/docs/ref/#state.Compartment) or
  with a top-level configuration
  [effect](https://codemirror.net/6/docs/ref/#state.StateEffect^reconfigure).
  */
  get reconfigured() {
    return this.startState.config != this.state.config;
  }
  /**
  Returns true if the transaction has a [user
  event](https://codemirror.net/6/docs/ref/#state.Transaction^userEvent) annotation that is equal to
  or more specific than `event`. For example, if the transaction
  has `"select.pointer"` as user event, `"select"` and
  `"select.pointer"` will match it.
  */
  isUserEvent(event) {
    let e = this.annotation(_Transaction.userEvent);
    return !!(e && (e == event || e.length > event.length && e.slice(0, event.length) == event && e[event.length] == "."));
  }
};
Transaction.time = /* @__PURE__ */ Annotation.define();
Transaction.userEvent = /* @__PURE__ */ Annotation.define();
Transaction.addToHistory = /* @__PURE__ */ Annotation.define();
Transaction.remote = /* @__PURE__ */ Annotation.define();
function joinRanges(a2, b) {
  let result = [];
  for (let iA = 0, iB = 0; ; ) {
    let from, to;
    if (iA < a2.length && (iB == b.length || b[iB] >= a2[iA])) {
      from = a2[iA++];
      to = a2[iA++];
    } else if (iB < b.length) {
      from = b[iB++];
      to = b[iB++];
    } else
      return result;
    if (!result.length || result[result.length - 1] < from)
      result.push(from, to);
    else if (result[result.length - 1] < to)
      result[result.length - 1] = to;
  }
}
function mergeTransaction(a2, b, sequential) {
  var _a2;
  let mapForA, mapForB, changes;
  if (sequential) {
    mapForA = b.changes;
    mapForB = ChangeSet.empty(b.changes.length);
    changes = a2.changes.compose(b.changes);
  } else {
    mapForA = b.changes.map(a2.changes);
    mapForB = a2.changes.mapDesc(b.changes, true);
    changes = a2.changes.compose(mapForA);
  }
  return {
    changes,
    selection: b.selection ? b.selection.map(mapForB) : (_a2 = a2.selection) === null || _a2 === void 0 ? void 0 : _a2.map(mapForA),
    effects: StateEffect.mapEffects(a2.effects, mapForA).concat(StateEffect.mapEffects(b.effects, mapForB)),
    annotations: a2.annotations.length ? a2.annotations.concat(b.annotations) : b.annotations,
    scrollIntoView: a2.scrollIntoView || b.scrollIntoView
  };
}
function resolveTransactionInner(state, spec, docSize) {
  let sel = spec.selection, annotations = asArray(spec.annotations);
  if (spec.userEvent)
    annotations = annotations.concat(Transaction.userEvent.of(spec.userEvent));
  return {
    changes: spec.changes instanceof ChangeSet ? spec.changes : ChangeSet.of(spec.changes || [], docSize, state.facet(lineSeparator)),
    selection: sel && (sel instanceof EditorSelection ? sel : EditorSelection.single(sel.anchor, sel.head)),
    effects: asArray(spec.effects),
    annotations,
    scrollIntoView: !!spec.scrollIntoView
  };
}
function resolveTransaction(state, specs, filter) {
  let s = resolveTransactionInner(state, specs.length ? specs[0] : {}, state.doc.length);
  if (specs.length && specs[0].filter === false)
    filter = false;
  for (let i = 1; i < specs.length; i++) {
    if (specs[i].filter === false)
      filter = false;
    let seq = !!specs[i].sequential;
    s = mergeTransaction(s, resolveTransactionInner(state, specs[i], seq ? s.changes.newLength : state.doc.length), seq);
  }
  let tr = Transaction.create(state, s.changes, s.selection, s.effects, s.annotations, s.scrollIntoView);
  return extendTransaction(filter ? filterTransaction(tr) : tr);
}
function filterTransaction(tr) {
  let state = tr.startState;
  let result = true;
  for (let filter of state.facet(changeFilter)) {
    let value = filter(tr);
    if (value === false) {
      result = false;
      break;
    }
    if (Array.isArray(value))
      result = result === true ? value : joinRanges(result, value);
  }
  if (result !== true) {
    let changes, back;
    if (result === false) {
      back = tr.changes.invertedDesc;
      changes = ChangeSet.empty(state.doc.length);
    } else {
      let filtered = tr.changes.filter(result);
      changes = filtered.changes;
      back = filtered.filtered.mapDesc(filtered.changes).invertedDesc;
    }
    tr = Transaction.create(state, changes, tr.selection && tr.selection.map(back), StateEffect.mapEffects(tr.effects, back), tr.annotations, tr.scrollIntoView);
  }
  let filters = state.facet(transactionFilter);
  for (let i = filters.length - 1; i >= 0; i--) {
    let filtered = filters[i](tr);
    if (filtered instanceof Transaction)
      tr = filtered;
    else if (Array.isArray(filtered) && filtered.length == 1 && filtered[0] instanceof Transaction)
      tr = filtered[0];
    else
      tr = resolveTransaction(state, asArray(filtered), false);
  }
  return tr;
}
function extendTransaction(tr) {
  let state = tr.startState, extenders = state.facet(transactionExtender), spec = tr;
  for (let i = extenders.length - 1; i >= 0; i--) {
    let extension = extenders[i](tr);
    if (extension && Object.keys(extension).length)
      spec = mergeTransaction(spec, resolveTransactionInner(state, extension, tr.changes.newLength), true);
  }
  return spec == tr ? tr : Transaction.create(state, tr.changes, tr.selection, spec.effects, spec.annotations, spec.scrollIntoView);
}
var none = [];
function asArray(value) {
  return value == null ? none : Array.isArray(value) ? value : [value];
}
var CharCategory = /* @__PURE__ */ (function(CharCategory2) {
  CharCategory2[CharCategory2["Word"] = 0] = "Word";
  CharCategory2[CharCategory2["Space"] = 1] = "Space";
  CharCategory2[CharCategory2["Other"] = 2] = "Other";
  return CharCategory2;
})(CharCategory || (CharCategory = {}));
var nonASCIISingleCaseWordChar = /[\u00df\u0587\u0590-\u05f4\u0600-\u06ff\u3040-\u309f\u30a0-\u30ff\u3400-\u4db5\u4e00-\u9fcc\uac00-\ud7af]/;
var wordChar;
try {
  wordChar = /* @__PURE__ */ new RegExp("[\\p{Alphabetic}\\p{Number}_]", "u");
} catch (_) {
}
function hasWordChar(str) {
  if (wordChar)
    return wordChar.test(str);
  for (let i = 0; i < str.length; i++) {
    let ch = str[i];
    if (/\w/.test(ch) || ch > "\x80" && (ch.toUpperCase() != ch.toLowerCase() || nonASCIISingleCaseWordChar.test(ch)))
      return true;
  }
  return false;
}
function makeCategorizer(wordChars) {
  return (char) => {
    if (!/\S/.test(char))
      return CharCategory.Space;
    if (hasWordChar(char))
      return CharCategory.Word;
    for (let i = 0; i < wordChars.length; i++)
      if (char.indexOf(wordChars[i]) > -1)
        return CharCategory.Word;
    return CharCategory.Other;
  };
}
var EditorState = class _EditorState {
  constructor(config, doc2, selection, values, computeSlot, tr) {
    this.config = config;
    this.doc = doc2;
    this.selection = selection;
    this.values = values;
    this.status = config.statusTemplate.slice();
    this.computeSlot = computeSlot;
    if (tr)
      tr._state = this;
    for (let i = 0; i < this.config.dynamicSlots.length; i++)
      ensureAddr(this, i << 1);
    this.computeSlot = null;
  }
  field(field, require2 = true) {
    let addr = this.config.address[field.id];
    if (addr == null) {
      if (require2)
        throw new RangeError("Field is not present in this state");
      return void 0;
    }
    ensureAddr(this, addr);
    return getAddr(this, addr);
  }
  /**
  Create a [transaction](https://codemirror.net/6/docs/ref/#state.Transaction) that updates this
  state. Any number of [transaction specs](https://codemirror.net/6/docs/ref/#state.TransactionSpec)
  can be passed. Unless
  [`sequential`](https://codemirror.net/6/docs/ref/#state.TransactionSpec.sequential) is set, the
  [changes](https://codemirror.net/6/docs/ref/#state.TransactionSpec.changes) (if any) of each spec
  are assumed to start in the _current_ document (not the document
  produced by previous specs), and its
  [selection](https://codemirror.net/6/docs/ref/#state.TransactionSpec.selection) and
  [effects](https://codemirror.net/6/docs/ref/#state.TransactionSpec.effects) are assumed to refer
  to the document created by its _own_ changes. The resulting
  transaction contains the combined effect of all the different
  specs. For [selection](https://codemirror.net/6/docs/ref/#state.TransactionSpec.selection), later
  specs take precedence over earlier ones.
  */
  update(...specs) {
    return resolveTransaction(this, specs, true);
  }
  /**
  @internal
  */
  applyTransaction(tr) {
    let conf = this.config, { base: base2, compartments } = conf;
    for (let effect of tr.effects) {
      if (effect.is(Compartment.reconfigure)) {
        if (conf) {
          compartments = /* @__PURE__ */ new Map();
          conf.compartments.forEach((val, key) => compartments.set(key, val));
          conf = null;
        }
        compartments.set(effect.value.compartment, effect.value.extension);
      } else if (effect.is(StateEffect.reconfigure)) {
        conf = null;
        base2 = effect.value;
      } else if (effect.is(StateEffect.appendConfig)) {
        conf = null;
        base2 = asArray(base2).concat(effect.value);
      }
    }
    let startValues;
    if (!conf) {
      conf = Configuration.resolve(base2, compartments, this);
      let intermediateState = new _EditorState(conf, this.doc, this.selection, conf.dynamicSlots.map(() => null), (state, slot) => slot.reconfigure(state, this), null);
      startValues = intermediateState.values;
    } else {
      startValues = tr.startState.values.slice();
    }
    let selection = tr.startState.facet(allowMultipleSelections) ? tr.newSelection : tr.newSelection.asSingle();
    new _EditorState(conf, tr.newDoc, selection, startValues, (state, slot) => slot.update(state, tr), tr);
  }
  /**
  Create a [transaction spec](https://codemirror.net/6/docs/ref/#state.TransactionSpec) that
  replaces every selection range with the given content.
  */
  replaceSelection(text) {
    if (typeof text == "string")
      text = this.toText(text);
    return this.changeByRange((range) => ({
      changes: { from: range.from, to: range.to, insert: text },
      range: EditorSelection.cursor(range.from + text.length)
    }));
  }
  /**
  Create a set of changes and a new selection by running the given
  function for each range in the active selection. The function
  can return an optional set of changes (in the coordinate space
  of the start document), plus an updated range (in the coordinate
  space of the document produced by the call's own changes). This
  method will merge all the changes and ranges into a single
  changeset and selection, and return it as a [transaction
  spec](https://codemirror.net/6/docs/ref/#state.TransactionSpec), which can be passed to
  [`update`](https://codemirror.net/6/docs/ref/#state.EditorState.update).
  */
  changeByRange(f) {
    let sel = this.selection;
    let result1 = f(sel.ranges[0]);
    let changes = this.changes(result1.changes), ranges = [result1.range];
    let effects = asArray(result1.effects);
    for (let i = 1; i < sel.ranges.length; i++) {
      let result = f(sel.ranges[i]);
      let newChanges = this.changes(result.changes), newMapped = newChanges.map(changes);
      for (let j = 0; j < i; j++)
        ranges[j] = ranges[j].map(newMapped);
      let mapBy = changes.mapDesc(newChanges, true);
      ranges.push(result.range.map(mapBy));
      changes = changes.compose(newMapped);
      effects = StateEffect.mapEffects(effects, newMapped).concat(StateEffect.mapEffects(asArray(result.effects), mapBy));
    }
    return {
      changes,
      selection: EditorSelection.create(ranges, sel.mainIndex),
      effects
    };
  }
  /**
  Create a [change set](https://codemirror.net/6/docs/ref/#state.ChangeSet) from the given change
  description, taking the state's document length and line
  separator into account.
  */
  changes(spec = []) {
    if (spec instanceof ChangeSet)
      return spec;
    return ChangeSet.of(spec, this.doc.length, this.facet(_EditorState.lineSeparator));
  }
  /**
  Using the state's [line
  separator](https://codemirror.net/6/docs/ref/#state.EditorState^lineSeparator), create a
  [`Text`](https://codemirror.net/6/docs/ref/#state.Text) instance from the given string.
  */
  toText(string2) {
    return Text.of(string2.split(this.facet(_EditorState.lineSeparator) || DefaultSplit));
  }
  /**
  Return the given range of the document as a string.
  */
  sliceDoc(from = 0, to = this.doc.length) {
    return this.doc.sliceString(from, to, this.lineBreak);
  }
  /**
  Get the value of a state [facet](https://codemirror.net/6/docs/ref/#state.Facet).
  */
  facet(facet) {
    let addr = this.config.address[facet.id];
    if (addr == null)
      return facet.default;
    ensureAddr(this, addr);
    return getAddr(this, addr);
  }
  /**
  Convert this state to a JSON-serializable object. When custom
  fields should be serialized, you can pass them in as an object
  mapping property names (in the resulting object, which should
  not use `doc` or `selection`) to fields.
  */
  toJSON(fields) {
    let result = {
      doc: this.sliceDoc(),
      selection: this.selection.toJSON()
    };
    if (fields)
      for (let prop in fields) {
        let value = fields[prop];
        if (value instanceof StateField && this.config.address[value.id] != null)
          result[prop] = value.spec.toJSON(this.field(fields[prop]), this);
      }
    return result;
  }
  /**
  Deserialize a state from its JSON representation. When custom
  fields should be deserialized, pass the same object you passed
  to [`toJSON`](https://codemirror.net/6/docs/ref/#state.EditorState.toJSON) when serializing as
  third argument.
  */
  static fromJSON(json, config = {}, fields) {
    if (!json || typeof json.doc != "string")
      throw new RangeError("Invalid JSON representation for EditorState");
    let fieldInit = [];
    if (fields)
      for (let prop in fields) {
        if (Object.prototype.hasOwnProperty.call(json, prop)) {
          let field = fields[prop], value = json[prop];
          fieldInit.push(field.init((state) => field.spec.fromJSON(value, state)));
        }
      }
    return _EditorState.create({
      doc: json.doc,
      selection: EditorSelection.fromJSON(json.selection),
      extensions: config.extensions ? fieldInit.concat([config.extensions]) : fieldInit
    });
  }
  /**
  Create a new state. You'll usually only need this when
  initializing an editor—updated states are created by applying
  transactions.
  */
  static create(config = {}) {
    let configuration = Configuration.resolve(config.extensions || [], /* @__PURE__ */ new Map());
    let doc2 = config.doc instanceof Text ? config.doc : Text.of((config.doc || "").split(configuration.staticFacet(_EditorState.lineSeparator) || DefaultSplit));
    let selection = !config.selection ? EditorSelection.single(0) : config.selection instanceof EditorSelection ? config.selection : EditorSelection.single(config.selection.anchor, config.selection.head);
    checkSelection(selection, doc2.length);
    if (!configuration.staticFacet(allowMultipleSelections))
      selection = selection.asSingle();
    return new _EditorState(configuration, doc2, selection, configuration.dynamicSlots.map(() => null), (state, slot) => slot.create(state), null);
  }
  /**
  The size (in columns) of a tab in the document, determined by
  the [`tabSize`](https://codemirror.net/6/docs/ref/#state.EditorState^tabSize) facet.
  */
  get tabSize() {
    return this.facet(_EditorState.tabSize);
  }
  /**
  Get the proper [line-break](https://codemirror.net/6/docs/ref/#state.EditorState^lineSeparator)
  string for this state.
  */
  get lineBreak() {
    return this.facet(_EditorState.lineSeparator) || "\n";
  }
  /**
  Returns true when the editor is
  [configured](https://codemirror.net/6/docs/ref/#state.EditorState^readOnly) to be read-only.
  */
  get readOnly() {
    return this.facet(readOnly);
  }
  /**
  Look up a translation for the given phrase (via the
  [`phrases`](https://codemirror.net/6/docs/ref/#state.EditorState^phrases) facet), or return the
  original string if no translation is found.
  
  If additional arguments are passed, they will be inserted in
  place of markers like `$1` (for the first value) and `$2`, etc.
  A single `$` is equivalent to `$1`, and `$$` will produce a
  literal dollar sign.
  */
  phrase(phrase, ...insert2) {
    for (let map of this.facet(_EditorState.phrases))
      if (Object.prototype.hasOwnProperty.call(map, phrase)) {
        phrase = map[phrase];
        break;
      }
    if (insert2.length)
      phrase = phrase.replace(/\$(\$|\d*)/g, (m, i) => {
        if (i == "$")
          return "$";
        let n = +(i || 1);
        return !n || n > insert2.length ? m : insert2[n - 1];
      });
    return phrase;
  }
  /**
  Find the values for a given language data field, provided by the
  the [`languageData`](https://codemirror.net/6/docs/ref/#state.EditorState^languageData) facet.
  
  Examples of language data fields are...
  
  - [`"commentTokens"`](https://codemirror.net/6/docs/ref/#commands.CommentTokens) for specifying
    comment syntax.
  - [`"autocomplete"`](https://codemirror.net/6/docs/ref/#autocomplete.autocompletion^config.override)
    for providing language-specific completion sources.
  - [`"wordChars"`](https://codemirror.net/6/docs/ref/#state.EditorState.charCategorizer) for adding
    characters that should be considered part of words in this
    language.
  - [`"closeBrackets"`](https://codemirror.net/6/docs/ref/#autocomplete.CloseBracketConfig) controls
    bracket closing behavior.
  */
  languageDataAt(name2, pos, side = -1) {
    let values = [];
    for (let provider of this.facet(languageData)) {
      for (let result of provider(this, pos, side)) {
        if (Object.prototype.hasOwnProperty.call(result, name2))
          values.push(result[name2]);
      }
    }
    return values;
  }
  /**
  Return a function that can categorize strings (expected to
  represent a single [grapheme cluster](https://codemirror.net/6/docs/ref/#state.findClusterBreak))
  into one of:
  
   - Word (contains an alphanumeric character or a character
     explicitly listed in the local language's `"wordChars"`
     language data, which should be a string)
   - Space (contains only whitespace)
   - Other (anything else)
  */
  charCategorizer(at) {
    return makeCategorizer(this.languageDataAt("wordChars", at).join(""));
  }
  /**
  Find the word at the given position, meaning the range
  containing all [word](https://codemirror.net/6/docs/ref/#state.CharCategory.Word) characters
  around it. If no word characters are adjacent to the position,
  this returns null.
  */
  wordAt(pos) {
    let { text, from, length } = this.doc.lineAt(pos);
    let cat = this.charCategorizer(pos);
    let start = pos - from, end = pos - from;
    while (start > 0) {
      let prev = findClusterBreak2(text, start, false);
      if (cat(text.slice(prev, start)) != CharCategory.Word)
        break;
      start = prev;
    }
    while (end < length) {
      let next = findClusterBreak2(text, end);
      if (cat(text.slice(end, next)) != CharCategory.Word)
        break;
      end = next;
    }
    return start == end ? null : EditorSelection.range(start + from, end + from);
  }
};
EditorState.allowMultipleSelections = allowMultipleSelections;
EditorState.tabSize = /* @__PURE__ */ Facet.define({
  combine: (values) => values.length ? values[0] : 4
});
EditorState.lineSeparator = lineSeparator;
EditorState.readOnly = readOnly;
EditorState.phrases = /* @__PURE__ */ Facet.define({
  compare(a2, b) {
    let kA = Object.keys(a2), kB = Object.keys(b);
    return kA.length == kB.length && kA.every((k) => a2[k] == b[k]);
  }
});
EditorState.languageData = languageData;
EditorState.changeFilter = changeFilter;
EditorState.transactionFilter = transactionFilter;
EditorState.transactionExtender = transactionExtender;
Compartment.reconfigure = /* @__PURE__ */ StateEffect.define();
var RangeValue = class {
  /**
  Compare this value with another value. Used when comparing
  rangesets. The default implementation compares by identity.
  Unless you are only creating a fixed number of unique instances
  of your value type, it is a good idea to implement this
  properly.
  */
  eq(other) {
    return this == other;
  }
  /**
  Create a [range](https://codemirror.net/6/docs/ref/#state.Range) with this value.
  */
  range(from, to = from) {
    return Range2.create(from, to, this);
  }
};
RangeValue.prototype.startSide = RangeValue.prototype.endSide = 0;
RangeValue.prototype.point = false;
RangeValue.prototype.mapMode = MapMode.TrackDel;
var Range2 = class _Range {
  constructor(from, to, value) {
    this.from = from;
    this.to = to;
    this.value = value;
  }
  /**
  @internal
  */
  static create(from, to, value) {
    return new _Range(from, to, value);
  }
};
function cmpRange(a2, b) {
  return a2.from - b.from || a2.value.startSide - b.value.startSide;
}
var Chunk = class _Chunk {
  constructor(from, to, value, maxPoint) {
    this.from = from;
    this.to = to;
    this.value = value;
    this.maxPoint = maxPoint;
  }
  get length() {
    return this.to[this.to.length - 1];
  }
  // Find the index of the given position and side. Use the ranges'
  // `from` pos when `end == false`, `to` when `end == true`.
  findIndex(pos, side, end, startAt = 0) {
    let arr = end ? this.to : this.from;
    for (let lo = startAt, hi = arr.length; ; ) {
      if (lo == hi)
        return lo;
      let mid = lo + hi >> 1;
      let diff = arr[mid] - pos || (end ? this.value[mid].endSide : this.value[mid].startSide) - side;
      if (mid == lo)
        return diff >= 0 ? lo : hi;
      if (diff >= 0)
        hi = mid;
      else
        lo = mid + 1;
    }
  }
  between(offset, from, to, f) {
    for (let i = this.findIndex(from, -1e9, true), e = this.findIndex(to, 1e9, false, i); i < e; i++)
      if (f(this.from[i] + offset, this.to[i] + offset, this.value[i]) === false)
        return false;
  }
  map(offset, changes) {
    let value = [], from = [], to = [], newPos = -1, maxPoint = -1;
    for (let i = 0; i < this.value.length; i++) {
      let val = this.value[i], curFrom = this.from[i] + offset, curTo = this.to[i] + offset, newFrom, newTo;
      if (curFrom == curTo) {
        let mapped = changes.mapPos(curFrom, val.startSide, val.mapMode);
        if (mapped == null)
          continue;
        newFrom = newTo = mapped;
        if (val.startSide != val.endSide) {
          newTo = changes.mapPos(curFrom, val.endSide);
          if (newTo < newFrom)
            continue;
        }
      } else {
        newFrom = changes.mapPos(curFrom, val.startSide);
        newTo = changes.mapPos(curTo, val.endSide);
        if (newFrom > newTo || newFrom == newTo && val.startSide > 0 && val.endSide <= 0)
          continue;
      }
      if ((newTo - newFrom || val.endSide - val.startSide) < 0)
        continue;
      if (newPos < 0)
        newPos = newFrom;
      if (val.point)
        maxPoint = Math.max(maxPoint, newTo - newFrom);
      value.push(val);
      from.push(newFrom - newPos);
      to.push(newTo - newPos);
    }
    return { mapped: value.length ? new _Chunk(from, to, value, maxPoint) : null, pos: newPos };
  }
};
var RangeSet = class _RangeSet {
  constructor(chunkPos, chunk, nextLayer, maxPoint) {
    this.chunkPos = chunkPos;
    this.chunk = chunk;
    this.nextLayer = nextLayer;
    this.maxPoint = maxPoint;
  }
  /**
  @internal
  */
  static create(chunkPos, chunk, nextLayer, maxPoint) {
    return new _RangeSet(chunkPos, chunk, nextLayer, maxPoint);
  }
  /**
  @internal
  */
  get length() {
    let last = this.chunk.length - 1;
    return last < 0 ? 0 : Math.max(this.chunkEnd(last), this.nextLayer.length);
  }
  /**
  The number of ranges in the set.
  */
  get size() {
    if (this.isEmpty)
      return 0;
    let size = this.nextLayer.size;
    for (let chunk of this.chunk)
      size += chunk.value.length;
    return size;
  }
  /**
  @internal
  */
  chunkEnd(index) {
    return this.chunkPos[index] + this.chunk[index].length;
  }
  /**
  Update the range set, optionally adding new ranges or filtering
  out existing ones.
  
  (Note: The type parameter is just there as a kludge to work
  around TypeScript variance issues that prevented `RangeSet<X>`
  from being a subtype of `RangeSet<Y>` when `X` is a subtype of
  `Y`.)
  */
  update(updateSpec) {
    let { add = [], sort = false, filterFrom = 0, filterTo = this.length } = updateSpec;
    let filter = updateSpec.filter;
    if (add.length == 0 && !filter)
      return this;
    if (sort)
      add = add.slice().sort(cmpRange);
    if (this.isEmpty)
      return add.length ? _RangeSet.of(add) : this;
    let cur = new LayerCursor(this, null, -1).goto(0), i = 0, spill = [];
    let builder = new RangeSetBuilder();
    while (cur.value || i < add.length) {
      if (i < add.length && (cur.from - add[i].from || cur.startSide - add[i].value.startSide) >= 0) {
        let range = add[i++];
        if (!builder.addInner(range.from, range.to, range.value))
          spill.push(range);
      } else if (cur.rangeIndex == 1 && cur.chunkIndex < this.chunk.length && (i == add.length || this.chunkEnd(cur.chunkIndex) < add[i].from) && (!filter || filterFrom > this.chunkEnd(cur.chunkIndex) || filterTo < this.chunkPos[cur.chunkIndex]) && builder.addChunk(this.chunkPos[cur.chunkIndex], this.chunk[cur.chunkIndex])) {
        cur.nextChunk();
      } else {
        if (!filter || filterFrom > cur.to || filterTo < cur.from || filter(cur.from, cur.to, cur.value)) {
          if (!builder.addInner(cur.from, cur.to, cur.value))
            spill.push(Range2.create(cur.from, cur.to, cur.value));
        }
        cur.next();
      }
    }
    return builder.finishInner(this.nextLayer.isEmpty && !spill.length ? _RangeSet.empty : this.nextLayer.update({ add: spill, filter, filterFrom, filterTo }));
  }
  /**
  Map this range set through a set of changes, return the new set.
  */
  map(changes) {
    if (changes.empty || this.isEmpty)
      return this;
    let chunks = [], chunkPos = [], maxPoint = -1;
    for (let i = 0; i < this.chunk.length; i++) {
      let start = this.chunkPos[i], chunk = this.chunk[i];
      let touch = changes.touchesRange(start, start + chunk.length);
      if (touch === false) {
        maxPoint = Math.max(maxPoint, chunk.maxPoint);
        chunks.push(chunk);
        chunkPos.push(changes.mapPos(start));
      } else if (touch === true) {
        let { mapped, pos } = chunk.map(start, changes);
        if (mapped) {
          maxPoint = Math.max(maxPoint, mapped.maxPoint);
          chunks.push(mapped);
          chunkPos.push(pos);
        }
      }
    }
    let next = this.nextLayer.map(changes);
    return chunks.length == 0 ? next : new _RangeSet(chunkPos, chunks, next || _RangeSet.empty, maxPoint);
  }
  /**
  Iterate over the ranges that touch the region `from` to `to`,
  calling `f` for each. There is no guarantee that the ranges will
  be reported in any specific order. When the callback returns
  `false`, iteration stops.
  */
  between(from, to, f) {
    if (this.isEmpty)
      return;
    for (let i = 0; i < this.chunk.length; i++) {
      let start = this.chunkPos[i], chunk = this.chunk[i];
      if (to >= start && from <= start + chunk.length && chunk.between(start, from - start, to - start, f) === false)
        return;
    }
    this.nextLayer.between(from, to, f);
  }
  /**
  Iterate over the ranges in this set, in order, including all
  ranges that end at or after `from`.
  */
  iter(from = 0) {
    return HeapCursor.from([this]).goto(from);
  }
  /**
  @internal
  */
  get isEmpty() {
    return this.nextLayer == this;
  }
  /**
  Iterate over the ranges in a collection of sets, in order,
  starting from `from`.
  */
  static iter(sets, from = 0) {
    return HeapCursor.from(sets).goto(from);
  }
  /**
  Iterate over two groups of sets, calling methods on `comparator`
  to notify it of possible differences.
  */
  static compare(oldSets, newSets, textDiff, comparator, minPointSize = -1) {
    let a2 = oldSets.filter((set) => set.maxPoint > 0 || !set.isEmpty && set.maxPoint >= minPointSize);
    let b = newSets.filter((set) => set.maxPoint > 0 || !set.isEmpty && set.maxPoint >= minPointSize);
    let sharedChunks = findSharedChunks(a2, b, textDiff);
    let sideA = new SpanCursor(a2, sharedChunks, minPointSize);
    let sideB = new SpanCursor(b, sharedChunks, minPointSize);
    textDiff.iterGaps((fromA, fromB, length) => compare(sideA, fromA, sideB, fromB, length, comparator));
    if (textDiff.empty && textDiff.length == 0)
      compare(sideA, 0, sideB, 0, 0, comparator);
  }
  /**
  Compare the contents of two groups of range sets, returning true
  if they are equivalent in the given range.
  */
  static eq(oldSets, newSets, from = 0, to) {
    if (to == null)
      to = 1e9 - 1;
    let a2 = oldSets.filter((set) => !set.isEmpty && newSets.indexOf(set) < 0);
    let b = newSets.filter((set) => !set.isEmpty && oldSets.indexOf(set) < 0);
    if (a2.length != b.length)
      return false;
    if (!a2.length)
      return true;
    let sharedChunks = findSharedChunks(a2, b);
    let sideA = new SpanCursor(a2, sharedChunks, 0).goto(from), sideB = new SpanCursor(b, sharedChunks, 0).goto(from);
    for (; ; ) {
      if (sideA.to != sideB.to || !sameValues(sideA.active, sideB.active) || sideA.point && (!sideB.point || !sideA.point.eq(sideB.point)))
        return false;
      if (sideA.to > to)
        return true;
      sideA.next();
      sideB.next();
    }
  }
  /**
  Iterate over a group of range sets at the same time, notifying
  the iterator about the ranges covering every given piece of
  content. Returns the open count (see
  [`SpanIterator.span`](https://codemirror.net/6/docs/ref/#state.SpanIterator.span)) at the end
  of the iteration.
  */
  static spans(sets, from, to, iterator, minPointSize = -1) {
    let cursor = new SpanCursor(sets, null, minPointSize).goto(from), pos = from;
    let openRanges = cursor.openStart;
    for (; ; ) {
      let curTo = Math.min(cursor.to, to);
      if (cursor.point) {
        let active = cursor.activeForPoint(cursor.to);
        let openCount = cursor.pointFrom < from ? active.length + 1 : cursor.point.startSide < 0 ? active.length : Math.min(active.length, openRanges);
        iterator.point(pos, curTo, cursor.point, active, openCount, cursor.pointRank);
        openRanges = Math.min(cursor.openEnd(curTo), active.length);
      } else if (curTo > pos) {
        iterator.span(pos, curTo, cursor.active, openRanges);
        openRanges = cursor.openEnd(curTo);
      }
      if (cursor.to > to)
        return openRanges + (cursor.point && cursor.to > to ? 1 : 0);
      pos = cursor.to;
      cursor.next();
    }
  }
  /**
  Create a range set for the given range or array of ranges. By
  default, this expects the ranges to be _sorted_ (by start
  position and, if two start at the same position,
  `value.startSide`). You can pass `true` as second argument to
  cause the method to sort them.
  */
  static of(ranges, sort = false) {
    let build = new RangeSetBuilder();
    for (let range of ranges instanceof Range2 ? [ranges] : sort ? lazySort(ranges) : ranges)
      build.add(range.from, range.to, range.value);
    return build.finish();
  }
  /**
  Join an array of range sets into a single set.
  */
  static join(sets) {
    if (!sets.length)
      return _RangeSet.empty;
    let result = sets[sets.length - 1];
    for (let i = sets.length - 2; i >= 0; i--) {
      for (let layer = sets[i]; layer != _RangeSet.empty; layer = layer.nextLayer)
        result = new _RangeSet(layer.chunkPos, layer.chunk, result, Math.max(layer.maxPoint, result.maxPoint));
    }
    return result;
  }
};
RangeSet.empty = /* @__PURE__ */ new RangeSet([], [], null, -1);
function lazySort(ranges) {
  if (ranges.length > 1)
    for (let prev = ranges[0], i = 1; i < ranges.length; i++) {
      let cur = ranges[i];
      if (cmpRange(prev, cur) > 0)
        return ranges.slice().sort(cmpRange);
      prev = cur;
    }
  return ranges;
}
RangeSet.empty.nextLayer = RangeSet.empty;
var RangeSetBuilder = class _RangeSetBuilder {
  finishChunk(newArrays) {
    this.chunks.push(new Chunk(this.from, this.to, this.value, this.maxPoint));
    this.chunkPos.push(this.chunkStart);
    this.chunkStart = -1;
    this.setMaxPoint = Math.max(this.setMaxPoint, this.maxPoint);
    this.maxPoint = -1;
    if (newArrays) {
      this.from = [];
      this.to = [];
      this.value = [];
    }
  }
  /**
  Create an empty builder.
  */
  constructor() {
    this.chunks = [];
    this.chunkPos = [];
    this.chunkStart = -1;
    this.last = null;
    this.lastFrom = -1e9;
    this.lastTo = -1e9;
    this.from = [];
    this.to = [];
    this.value = [];
    this.maxPoint = -1;
    this.setMaxPoint = -1;
    this.nextLayer = null;
  }
  /**
  Add a range. Ranges should be added in sorted (by `from` and
  `value.startSide`) order.
  */
  add(from, to, value) {
    if (!this.addInner(from, to, value))
      (this.nextLayer || (this.nextLayer = new _RangeSetBuilder())).add(from, to, value);
  }
  /**
  @internal
  */
  addInner(from, to, value) {
    let diff = from - this.lastTo || value.startSide - this.last.endSide;
    if (diff <= 0 && (from - this.lastFrom || value.startSide - this.last.startSide) < 0)
      throw new Error("Ranges must be added sorted by `from` position and `startSide`");
    if (diff < 0)
      return false;
    if (this.from.length == 250)
      this.finishChunk(true);
    if (this.chunkStart < 0)
      this.chunkStart = from;
    this.from.push(from - this.chunkStart);
    this.to.push(to - this.chunkStart);
    this.last = value;
    this.lastFrom = from;
    this.lastTo = to;
    this.value.push(value);
    if (value.point)
      this.maxPoint = Math.max(this.maxPoint, to - from);
    return true;
  }
  /**
  @internal
  */
  addChunk(from, chunk) {
    if ((from - this.lastTo || chunk.value[0].startSide - this.last.endSide) < 0)
      return false;
    if (this.from.length)
      this.finishChunk(true);
    this.setMaxPoint = Math.max(this.setMaxPoint, chunk.maxPoint);
    this.chunks.push(chunk);
    this.chunkPos.push(from);
    let last = chunk.value.length - 1;
    this.last = chunk.value[last];
    this.lastFrom = chunk.from[last] + from;
    this.lastTo = chunk.to[last] + from;
    return true;
  }
  /**
  Finish the range set. Returns the new set. The builder can't be
  used anymore after this has been called.
  */
  finish() {
    return this.finishInner(RangeSet.empty);
  }
  /**
  @internal
  */
  finishInner(next) {
    if (this.from.length)
      this.finishChunk(false);
    if (this.chunks.length == 0)
      return next;
    let result = RangeSet.create(this.chunkPos, this.chunks, this.nextLayer ? this.nextLayer.finishInner(next) : next, this.setMaxPoint);
    this.from = null;
    return result;
  }
};
function findSharedChunks(a2, b, textDiff) {
  let inA = /* @__PURE__ */ new Map();
  for (let set of a2)
    for (let i = 0; i < set.chunk.length; i++)
      if (set.chunk[i].maxPoint <= 0)
        inA.set(set.chunk[i], set.chunkPos[i]);
  let shared = /* @__PURE__ */ new Set();
  for (let set of b)
    for (let i = 0; i < set.chunk.length; i++) {
      let known = inA.get(set.chunk[i]);
      if (known != null && (textDiff ? textDiff.mapPos(known) : known) == set.chunkPos[i] && !(textDiff === null || textDiff === void 0 ? void 0 : textDiff.touchesRange(known, known + set.chunk[i].length)))
        shared.add(set.chunk[i]);
    }
  return shared;
}
var LayerCursor = class {
  constructor(layer, skip, minPoint, rank = 0) {
    this.layer = layer;
    this.skip = skip;
    this.minPoint = minPoint;
    this.rank = rank;
  }
  get startSide() {
    return this.value ? this.value.startSide : 0;
  }
  get endSide() {
    return this.value ? this.value.endSide : 0;
  }
  goto(pos, side = -1e9) {
    this.chunkIndex = this.rangeIndex = 0;
    this.gotoInner(pos, side, false);
    return this;
  }
  gotoInner(pos, side, forward) {
    while (this.chunkIndex < this.layer.chunk.length) {
      let next = this.layer.chunk[this.chunkIndex];
      if (!(this.skip && this.skip.has(next) || this.layer.chunkEnd(this.chunkIndex) < pos || next.maxPoint < this.minPoint))
        break;
      this.chunkIndex++;
      forward = false;
    }
    if (this.chunkIndex < this.layer.chunk.length) {
      let rangeIndex = this.layer.chunk[this.chunkIndex].findIndex(pos - this.layer.chunkPos[this.chunkIndex], side, true);
      if (!forward || this.rangeIndex < rangeIndex)
        this.setRangeIndex(rangeIndex);
    }
    this.next();
  }
  forward(pos, side) {
    if ((this.to - pos || this.endSide - side) < 0)
      this.gotoInner(pos, side, true);
  }
  next() {
    for (; ; ) {
      if (this.chunkIndex == this.layer.chunk.length) {
        this.from = this.to = 1e9;
        this.value = null;
        break;
      } else {
        let chunkPos = this.layer.chunkPos[this.chunkIndex], chunk = this.layer.chunk[this.chunkIndex];
        let from = chunkPos + chunk.from[this.rangeIndex];
        this.from = from;
        this.to = chunkPos + chunk.to[this.rangeIndex];
        this.value = chunk.value[this.rangeIndex];
        this.setRangeIndex(this.rangeIndex + 1);
        if (this.minPoint < 0 || this.value.point && this.to - this.from >= this.minPoint)
          break;
      }
    }
  }
  setRangeIndex(index) {
    if (index == this.layer.chunk[this.chunkIndex].value.length) {
      this.chunkIndex++;
      if (this.skip) {
        while (this.chunkIndex < this.layer.chunk.length && this.skip.has(this.layer.chunk[this.chunkIndex]))
          this.chunkIndex++;
      }
      this.rangeIndex = 0;
    } else {
      this.rangeIndex = index;
    }
  }
  nextChunk() {
    this.chunkIndex++;
    this.rangeIndex = 0;
    this.next();
  }
  compare(other) {
    return this.from - other.from || this.startSide - other.startSide || this.rank - other.rank || this.to - other.to || this.endSide - other.endSide;
  }
};
var HeapCursor = class _HeapCursor {
  constructor(heap) {
    this.heap = heap;
  }
  static from(sets, skip = null, minPoint = -1) {
    let heap = [];
    for (let i = 0; i < sets.length; i++) {
      for (let cur = sets[i]; !cur.isEmpty; cur = cur.nextLayer) {
        if (cur.maxPoint >= minPoint)
          heap.push(new LayerCursor(cur, skip, minPoint, i));
      }
    }
    return heap.length == 1 ? heap[0] : new _HeapCursor(heap);
  }
  get startSide() {
    return this.value ? this.value.startSide : 0;
  }
  goto(pos, side = -1e9) {
    for (let cur of this.heap)
      cur.goto(pos, side);
    for (let i = this.heap.length >> 1; i >= 0; i--)
      heapBubble(this.heap, i);
    this.next();
    return this;
  }
  forward(pos, side) {
    for (let cur of this.heap)
      cur.forward(pos, side);
    for (let i = this.heap.length >> 1; i >= 0; i--)
      heapBubble(this.heap, i);
    if ((this.to - pos || this.value.endSide - side) < 0)
      this.next();
  }
  next() {
    if (this.heap.length == 0) {
      this.from = this.to = 1e9;
      this.value = null;
      this.rank = -1;
    } else {
      let top2 = this.heap[0];
      this.from = top2.from;
      this.to = top2.to;
      this.value = top2.value;
      this.rank = top2.rank;
      if (top2.value)
        top2.next();
      heapBubble(this.heap, 0);
    }
  }
};
function heapBubble(heap, index) {
  for (let cur = heap[index]; ; ) {
    let childIndex = (index << 1) + 1;
    if (childIndex >= heap.length)
      break;
    let child = heap[childIndex];
    if (childIndex + 1 < heap.length && child.compare(heap[childIndex + 1]) >= 0) {
      child = heap[childIndex + 1];
      childIndex++;
    }
    if (cur.compare(child) < 0)
      break;
    heap[childIndex] = cur;
    heap[index] = child;
    index = childIndex;
  }
}
var SpanCursor = class {
  constructor(sets, skip, minPoint) {
    this.minPoint = minPoint;
    this.active = [];
    this.activeTo = [];
    this.activeRank = [];
    this.minActive = -1;
    this.point = null;
    this.pointFrom = 0;
    this.pointRank = 0;
    this.to = -1e9;
    this.endSide = 0;
    this.openStart = -1;
    this.cursor = HeapCursor.from(sets, skip, minPoint);
  }
  goto(pos, side = -1e9) {
    this.cursor.goto(pos, side);
    this.active.length = this.activeTo.length = this.activeRank.length = 0;
    this.minActive = -1;
    this.to = pos;
    this.endSide = side;
    this.openStart = -1;
    this.next();
    return this;
  }
  forward(pos, side) {
    while (this.minActive > -1 && (this.activeTo[this.minActive] - pos || this.active[this.minActive].endSide - side) < 0)
      this.removeActive(this.minActive);
    this.cursor.forward(pos, side);
  }
  removeActive(index) {
    remove(this.active, index);
    remove(this.activeTo, index);
    remove(this.activeRank, index);
    this.minActive = findMinIndex(this.active, this.activeTo);
  }
  addActive(trackOpen) {
    let i = 0, { value, to, rank } = this.cursor;
    while (i < this.activeRank.length && (rank - this.activeRank[i] || to - this.activeTo[i]) > 0)
      i++;
    insert(this.active, i, value);
    insert(this.activeTo, i, to);
    insert(this.activeRank, i, rank);
    if (trackOpen)
      insert(trackOpen, i, this.cursor.from);
    this.minActive = findMinIndex(this.active, this.activeTo);
  }
  // After calling this, if `this.point` != null, the next range is a
  // point. Otherwise, it's a regular range, covered by `this.active`.
  next() {
    let from = this.to, wasPoint = this.point;
    this.point = null;
    let trackOpen = this.openStart < 0 ? [] : null;
    for (; ; ) {
      let a2 = this.minActive;
      if (a2 > -1 && (this.activeTo[a2] - this.cursor.from || this.active[a2].endSide - this.cursor.startSide) < 0) {
        if (this.activeTo[a2] > from) {
          this.to = this.activeTo[a2];
          this.endSide = this.active[a2].endSide;
          break;
        }
        this.removeActive(a2);
        if (trackOpen)
          remove(trackOpen, a2);
      } else if (!this.cursor.value) {
        this.to = this.endSide = 1e9;
        break;
      } else if (this.cursor.from > from) {
        this.to = this.cursor.from;
        this.endSide = this.cursor.startSide;
        break;
      } else {
        let nextVal = this.cursor.value;
        if (!nextVal.point) {
          this.addActive(trackOpen);
          this.cursor.next();
        } else if (wasPoint && this.cursor.to == this.to && this.cursor.from < this.cursor.to) {
          this.cursor.next();
        } else {
          this.point = nextVal;
          this.pointFrom = this.cursor.from;
          this.pointRank = this.cursor.rank;
          this.to = this.cursor.to;
          this.endSide = nextVal.endSide;
          this.cursor.next();
          this.forward(this.to, this.endSide);
          break;
        }
      }
    }
    if (trackOpen) {
      this.openStart = 0;
      for (let i = trackOpen.length - 1; i >= 0 && trackOpen[i] < from; i--)
        this.openStart++;
    }
  }
  activeForPoint(to) {
    if (!this.active.length)
      return this.active;
    let active = [];
    for (let i = this.active.length - 1; i >= 0; i--) {
      if (this.activeRank[i] < this.pointRank)
        break;
      if (this.activeTo[i] > to || this.activeTo[i] == to && this.active[i].endSide >= this.point.endSide)
        active.push(this.active[i]);
    }
    return active.reverse();
  }
  openEnd(to) {
    let open = 0;
    for (let i = this.activeTo.length - 1; i >= 0 && this.activeTo[i] > to; i--)
      open++;
    return open;
  }
};
function compare(a2, startA, b, startB, length, comparator) {
  a2.goto(startA);
  b.goto(startB);
  let endB = startB + length;
  let pos = startB, dPos = startB - startA;
  for (; ; ) {
    let dEnd = a2.to + dPos - b.to, diff = dEnd || a2.endSide - b.endSide;
    let end = diff < 0 ? a2.to + dPos : b.to, clipEnd = Math.min(end, endB);
    if (a2.point || b.point) {
      if (!(a2.point && b.point && (a2.point == b.point || a2.point.eq(b.point)) && sameValues(a2.activeForPoint(a2.to), b.activeForPoint(b.to))))
        comparator.comparePoint(pos, clipEnd, a2.point, b.point);
    } else {
      if (clipEnd > pos && !sameValues(a2.active, b.active))
        comparator.compareRange(pos, clipEnd, a2.active, b.active);
    }
    if (end > endB)
      break;
    if ((dEnd || a2.openEnd != b.openEnd) && comparator.boundChange)
      comparator.boundChange(end);
    pos = end;
    if (diff <= 0)
      a2.next();
    if (diff >= 0)
      b.next();
  }
}
function sameValues(a2, b) {
  if (a2.length != b.length)
    return false;
  for (let i = 0; i < a2.length; i++)
    if (a2[i] != b[i] && !a2[i].eq(b[i]))
      return false;
  return true;
}
function remove(array, index) {
  for (let i = index, e = array.length - 1; i < e; i++)
    array[i] = array[i + 1];
  array.pop();
}
function insert(array, index, value) {
  for (let i = array.length - 1; i >= index; i--)
    array[i + 1] = array[i];
  array[index] = value;
}
function findMinIndex(value, array) {
  let found = -1, foundPos = 1e9;
  for (let i = 0; i < array.length; i++)
    if ((array[i] - foundPos || value[i].endSide - value[found].endSide) < 0) {
      found = i;
      foundPos = array[i];
    }
  return found;
}
function findColumn(string2, col, tabSize, strict) {
  for (let i = 0, n = 0; ; ) {
    if (n >= col)
      return i;
    if (i == string2.length)
      break;
    n += string2.charCodeAt(i) == 9 ? tabSize - n % tabSize : 1;
    i = findClusterBreak2(string2, i);
  }
  return strict === true ? -1 : string2.length;
}

// node_modules/style-mod/src/style-mod.js
var C = "\u037C";
var COUNT = typeof Symbol == "undefined" ? "__" + C : Symbol.for(C);
var SET = typeof Symbol == "undefined" ? "__styleSet" + Math.floor(Math.random() * 1e8) : /* @__PURE__ */ Symbol("styleSet");
var top = typeof globalThis != "undefined" ? globalThis : typeof window != "undefined" ? window : {};
var StyleModule = class {
  // :: (Object<Style>, ?{finish: ?(string) → string})
  // Create a style module from the given spec.
  //
  // When `finish` is given, it is called on regular (non-`@`)
  // selectors (after `&` expansion) to compute the final selector.
  constructor(spec, options) {
    this.rules = [];
    let { finish } = options || {};
    function splitSelector(selector) {
      return /^@/.test(selector) ? [selector] : selector.split(/,\s*/);
    }
    function render(selectors, spec2, target, isKeyframes) {
      let local = [], isAt = /^@(\w+)\b/.exec(selectors[0]), keyframes = isAt && isAt[1] == "keyframes";
      if (isAt && spec2 == null) return target.push(selectors[0] + ";");
      for (let prop in spec2) {
        let value = spec2[prop];
        if (/&/.test(prop)) {
          render(
            prop.split(/,\s*/).map((part) => selectors.map((sel) => part.replace(/&/, sel))).reduce((a2, b) => a2.concat(b)),
            value,
            target
          );
        } else if (value && typeof value == "object") {
          if (!isAt) throw new RangeError("The value of a property (" + prop + ") should be a primitive value.");
          render(splitSelector(prop), value, local, keyframes);
        } else if (value != null) {
          local.push(prop.replace(/_.*/, "").replace(/[A-Z]/g, (l) => "-" + l.toLowerCase()) + ": " + value + ";");
        }
      }
      if (local.length || keyframes) {
        target.push((finish && !isAt && !isKeyframes ? selectors.map(finish) : selectors).join(", ") + " {" + local.join(" ") + "}");
      }
    }
    for (let prop in spec) render(splitSelector(prop), spec[prop], this.rules);
  }
  // :: () → string
  // Returns a string containing the module's CSS rules.
  getRules() {
    return this.rules.join("\n");
  }
  // :: () → string
  // Generate a new unique CSS class name.
  static newName() {
    let id = top[COUNT] || 1;
    top[COUNT] = id + 1;
    return C + id.toString(36);
  }
  // :: (union<Document, ShadowRoot>, union<[StyleModule], StyleModule>, ?{nonce: ?string})
  //
  // Mount the given set of modules in the given DOM root, which ensures
  // that the CSS rules defined by the module are available in that
  // context.
  //
  // Rules are only added to the document once per root.
  //
  // Rule order will follow the order of the modules, so that rules from
  // modules later in the array take precedence of those from earlier
  // modules. If you call this function multiple times for the same root
  // in a way that changes the order of already mounted modules, the old
  // order will be changed.
  //
  // If a Content Security Policy nonce is provided, it is added to
  // the `<style>` tag generated by the library.
  static mount(root, modules, options) {
    let set = root[SET], nonce = options && options.nonce;
    if (!set) set = new StyleSet(root, nonce);
    else if (nonce) set.setNonce(nonce);
    set.mount(Array.isArray(modules) ? modules : [modules], root);
  }
};
var adoptedSet = /* @__PURE__ */ new Map();
var StyleSet = class {
  constructor(root, nonce) {
    let doc2 = root.ownerDocument || root, win = doc2.defaultView;
    if (!root.head && root.adoptedStyleSheets && win.CSSStyleSheet) {
      let adopted = adoptedSet.get(doc2);
      if (adopted) return root[SET] = adopted;
      this.sheet = new win.CSSStyleSheet();
      adoptedSet.set(doc2, this);
    } else {
      this.styleTag = doc2.createElement("style");
      if (nonce) this.styleTag.setAttribute("nonce", nonce);
    }
    this.modules = [];
    root[SET] = this;
  }
  mount(modules, root) {
    let sheet = this.sheet;
    let pos = 0, j = 0;
    for (let i = 0; i < modules.length; i++) {
      let mod = modules[i], index = this.modules.indexOf(mod);
      if (index < j && index > -1) {
        this.modules.splice(index, 1);
        j--;
        index = -1;
      }
      if (index == -1) {
        this.modules.splice(j++, 0, mod);
        if (sheet) for (let k = 0; k < mod.rules.length; k++)
          sheet.insertRule(mod.rules[k], pos++);
      } else {
        while (j < index) pos += this.modules[j++].rules.length;
        pos += mod.rules.length;
        j++;
      }
    }
    if (sheet) {
      if (root.adoptedStyleSheets.indexOf(this.sheet) < 0)
        root.adoptedStyleSheets = [this.sheet, ...root.adoptedStyleSheets];
    } else {
      let text = "";
      for (let i = 0; i < this.modules.length; i++)
        text += this.modules[i].getRules() + "\n";
      this.styleTag.textContent = text;
      let target = root.head || root;
      if (this.styleTag.parentNode != target)
        target.insertBefore(this.styleTag, target.firstChild);
    }
  }
  setNonce(nonce) {
    if (this.styleTag && this.styleTag.getAttribute("nonce") != nonce)
      this.styleTag.setAttribute("nonce", nonce);
  }
};

// node_modules/w3c-keyname/index.js
var base = {
  8: "Backspace",
  9: "Tab",
  10: "Enter",
  12: "NumLock",
  13: "Enter",
  16: "Shift",
  17: "Control",
  18: "Alt",
  20: "CapsLock",
  27: "Escape",
  32: " ",
  33: "PageUp",
  34: "PageDown",
  35: "End",
  36: "Home",
  37: "ArrowLeft",
  38: "ArrowUp",
  39: "ArrowRight",
  40: "ArrowDown",
  44: "PrintScreen",
  45: "Insert",
  46: "Delete",
  59: ";",
  61: "=",
  91: "Meta",
  92: "Meta",
  106: "*",
  107: "+",
  108: ",",
  109: "-",
  110: ".",
  111: "/",
  144: "NumLock",
  145: "ScrollLock",
  160: "Shift",
  161: "Shift",
  162: "Control",
  163: "Control",
  164: "Alt",
  165: "Alt",
  173: "-",
  186: ";",
  187: "=",
  188: ",",
  189: "-",
  190: ".",
  191: "/",
  192: "`",
  219: "[",
  220: "\\",
  221: "]",
  222: "'"
};
var shift = {
  48: ")",
  49: "!",
  50: "@",
  51: "#",
  52: "$",
  53: "%",
  54: "^",
  55: "&",
  56: "*",
  57: "(",
  59: ":",
  61: "+",
  173: "_",
  186: ":",
  187: "+",
  188: "<",
  189: "_",
  190: ">",
  191: "?",
  192: "~",
  219: "{",
  220: "|",
  221: "}",
  222: '"'
};
var mac = typeof navigator != "undefined" && /Mac/.test(navigator.platform);
var ie = typeof navigator != "undefined" && /MSIE \d|Trident\/(?:[7-9]|\d{2,})\..*rv:(\d+)/.exec(navigator.userAgent);
for (i = 0; i < 10; i++) base[48 + i] = base[96 + i] = String(i);
var i;
for (i = 1; i <= 24; i++) base[i + 111] = "F" + i;
var i;
for (i = 65; i <= 90; i++) {
  base[i] = String.fromCharCode(i + 32);
  shift[i] = String.fromCharCode(i);
}
var i;
for (code in base) if (!shift.hasOwnProperty(code)) shift[code] = base[code];
var code;

// node_modules/@codemirror/view/dist/index.js
var nav = typeof navigator != "undefined" ? navigator : { userAgent: "", vendor: "", platform: "" };
var doc = typeof document != "undefined" ? document : { documentElement: { style: {} } };
var ie_edge = /* @__PURE__ */ /Edge\/(\d+)/.exec(nav.userAgent);
var ie_upto10 = /* @__PURE__ */ /MSIE \d/.test(nav.userAgent);
var ie_11up = /* @__PURE__ */ /Trident\/(?:[7-9]|\d{2,})\..*rv:(\d+)/.exec(nav.userAgent);
var ie2 = !!(ie_upto10 || ie_11up || ie_edge);
var gecko = !ie2 && /* @__PURE__ */ /gecko\/(\d+)/i.test(nav.userAgent);
var chrome = !ie2 && /* @__PURE__ */ /Chrome\/(\d+)/.exec(nav.userAgent);
var webkit = "webkitFontSmoothing" in doc.documentElement.style;
var safari = !ie2 && /* @__PURE__ */ /Apple Computer/.test(nav.vendor);
var ios = safari && (/* @__PURE__ */ /Mobile\/\w+/.test(nav.userAgent) || nav.maxTouchPoints > 2);
var browser = {
  mac: ios || /* @__PURE__ */ /Mac/.test(nav.platform),
  windows: /* @__PURE__ */ /Win/.test(nav.platform),
  linux: /* @__PURE__ */ /Linux|X11/.test(nav.platform),
  ie: ie2,
  ie_version: ie_upto10 ? doc.documentMode || 6 : ie_11up ? +ie_11up[1] : ie_edge ? +ie_edge[1] : 0,
  gecko,
  gecko_version: gecko ? +(/* @__PURE__ */ /Firefox\/(\d+)/.exec(nav.userAgent) || [0, 0])[1] : 0,
  chrome: !!chrome,
  chrome_version: chrome ? +chrome[1] : 0,
  ios,
  android: /* @__PURE__ */ /Android\b/.test(nav.userAgent),
  webkit,
  webkit_version: webkit ? +(/* @__PURE__ */ /\bAppleWebKit\/(\d+)/.exec(nav.userAgent) || [0, 0])[1] : 0,
  safari,
  safari_version: safari ? +(/* @__PURE__ */ /\bVersion\/(\d+(\.\d+)?)/.exec(nav.userAgent) || [0, 0])[1] : 0,
  tabSize: doc.documentElement.style.tabSize != null ? "tab-size" : "-moz-tab-size"
};
function combineAttrs(source, target) {
  for (let name2 in source) {
    if (name2 == "class" && target.class)
      target.class += " " + source.class;
    else if (name2 == "style" && target.style)
      target.style += ";" + source.style;
    else
      target[name2] = source[name2];
  }
  return target;
}
var noAttrs = /* @__PURE__ */ Object.create(null);
function attrsEq(a2, b, ignore) {
  if (a2 == b)
    return true;
  if (!a2)
    a2 = noAttrs;
  if (!b)
    b = noAttrs;
  let keysA = Object.keys(a2), keysB = Object.keys(b);
  if (keysA.length - (ignore && keysA.indexOf(ignore) > -1 ? 1 : 0) != keysB.length - (ignore && keysB.indexOf(ignore) > -1 ? 1 : 0))
    return false;
  for (let key of keysA) {
    if (key != ignore && (keysB.indexOf(key) == -1 || a2[key] !== b[key]))
      return false;
  }
  return true;
}
function setAttrs(dom, attrs) {
  for (let i = dom.attributes.length - 1; i >= 0; i--) {
    let name2 = dom.attributes[i].name;
    if (attrs[name2] == null)
      dom.removeAttribute(name2);
  }
  for (let name2 in attrs) {
    let value = attrs[name2];
    if (name2 == "style")
      dom.style.cssText = value;
    else if (dom.getAttribute(name2) != value)
      dom.setAttribute(name2, value);
  }
}
function updateAttrs(dom, prev, attrs) {
  let changed = false;
  if (prev) {
    for (let name2 in prev)
      if (!(attrs && name2 in attrs)) {
        changed = true;
        if (name2 == "style")
          dom.style.cssText = "";
        else
          dom.removeAttribute(name2);
      }
  }
  if (attrs) {
    for (let name2 in attrs)
      if (!(prev && prev[name2] == attrs[name2])) {
        changed = true;
        if (name2 == "style")
          dom.style.cssText = attrs[name2];
        else
          dom.setAttribute(name2, attrs[name2]);
      }
  }
  return changed;
}
function getAttrs(dom) {
  let attrs = /* @__PURE__ */ Object.create(null);
  for (let i = 0; i < dom.attributes.length; i++) {
    let attr = dom.attributes[i];
    attrs[attr.name] = attr.value;
  }
  return attrs;
}
var WidgetType = class {
  /**
  Compare this instance to another instance of the same type.
  (TypeScript can't express this, but only instances of the same
  specific class will be passed to this method.) This is used to
  avoid redrawing widgets when they are replaced by a new
  decoration of the same type. The default implementation just
  returns `false`, which will cause new instances of the widget to
  always be redrawn.
  */
  eq(widget) {
    return false;
  }
  /**
  Update a DOM element created by a widget of the same type (but
  different, non-`eq` content) to reflect this widget. May return
  true to indicate that it could update, false to indicate it
  couldn't (in which case the widget will be redrawn). The default
  implementation just returns false.
  */
  updateDOM(dom, view) {
    return false;
  }
  /**
  @internal
  */
  compare(other) {
    return this == other || this.constructor == other.constructor && this.eq(other);
  }
  /**
  The estimated height this widget will have, to be used when
  estimating the height of content that hasn't been drawn. May
  return -1 to indicate you don't know. The default implementation
  returns -1.
  */
  get estimatedHeight() {
    return -1;
  }
  /**
  For inline widgets that are displayed inline (as opposed to
  `inline-block`) and introduce line breaks (through `<br>` tags
  or textual newlines), this must indicate the amount of line
  breaks they introduce. Defaults to 0.
  */
  get lineBreaks() {
    return 0;
  }
  /**
  Can be used to configure which kinds of events inside the widget
  should be ignored by the editor. The default is to ignore all
  events.
  */
  ignoreEvent(event) {
    return true;
  }
  /**
  Override the way screen coordinates for positions at/in the
  widget are found. `pos` will be the offset into the widget, and
  `side` the side of the position that is being queried—less than
  zero for before, greater than zero for after, and zero for
  directly at that position.
  */
  coordsAt(dom, pos, side) {
    return null;
  }
  /**
  @internal
  */
  get isHidden() {
    return false;
  }
  /**
  @internal
  */
  get editable() {
    return false;
  }
  /**
  This is called when the an instance of the widget is removed
  from the editor view.
  */
  destroy(dom) {
  }
};
var BlockType = /* @__PURE__ */ (function(BlockType2) {
  BlockType2[BlockType2["Text"] = 0] = "Text";
  BlockType2[BlockType2["WidgetBefore"] = 1] = "WidgetBefore";
  BlockType2[BlockType2["WidgetAfter"] = 2] = "WidgetAfter";
  BlockType2[BlockType2["WidgetRange"] = 3] = "WidgetRange";
  return BlockType2;
})(BlockType || (BlockType = {}));
var Decoration = class extends RangeValue {
  constructor(startSide, endSide, widget, spec) {
    super();
    this.startSide = startSide;
    this.endSide = endSide;
    this.widget = widget;
    this.spec = spec;
  }
  /**
  @internal
  */
  get heightRelevant() {
    return false;
  }
  /**
  Create a mark decoration, which influences the styling of the
  content in its range. Nested mark decorations will cause nested
  DOM elements to be created. Nesting order is determined by
  precedence of the [facet](https://codemirror.net/6/docs/ref/#view.EditorView^decorations), with
  the higher-precedence decorations creating the inner DOM nodes.
  Such elements are split on line boundaries and on the boundaries
  of lower-precedence decorations.
  */
  static mark(spec) {
    return new MarkDecoration(spec);
  }
  /**
  Create a widget decoration, which displays a DOM element at the
  given position.
  */
  static widget(spec) {
    let side = Math.max(-1e4, Math.min(1e4, spec.side || 0)), block = !!spec.block;
    side += block && !spec.inlineOrder ? side > 0 ? 3e8 : -4e8 : side > 0 ? 1e8 : -1e8;
    return new PointDecoration(spec, side, side, block, spec.widget || null, false);
  }
  /**
  Create a replace decoration which replaces the given range with
  a widget, or simply hides it.
  */
  static replace(spec) {
    let block = !!spec.block, startSide, endSide;
    if (spec.isBlockGap) {
      startSide = -5e8;
      endSide = 4e8;
    } else {
      let { start, end } = getInclusive(spec, block);
      startSide = (start ? block ? -3e8 : -1 : 5e8) - 1;
      endSide = (end ? block ? 2e8 : 1 : -6e8) + 1;
    }
    return new PointDecoration(spec, startSide, endSide, block, spec.widget || null, true);
  }
  /**
  Create a line decoration, which can add DOM attributes to the
  line starting at the given position.
  */
  static line(spec) {
    return new LineDecoration(spec);
  }
  /**
  Build a [`DecorationSet`](https://codemirror.net/6/docs/ref/#view.DecorationSet) from the given
  decorated range or ranges. If the ranges aren't already sorted,
  pass `true` for `sort` to make the library sort them for you.
  */
  static set(of, sort = false) {
    return RangeSet.of(of, sort);
  }
  /**
  @internal
  */
  hasHeight() {
    return this.widget ? this.widget.estimatedHeight > -1 : false;
  }
};
Decoration.none = RangeSet.empty;
var MarkDecoration = class _MarkDecoration extends Decoration {
  constructor(spec) {
    let { start, end } = getInclusive(spec);
    super(start ? -1 : 5e8, end ? 1 : -6e8, null, spec);
    this.tagName = spec.tagName || "span";
    this.attrs = spec.class && spec.attributes ? combineAttrs(spec.attributes, { class: spec.class }) : spec.class ? { class: spec.class } : spec.attributes || noAttrs;
  }
  eq(other) {
    return this == other || other instanceof _MarkDecoration && this.tagName == other.tagName && attrsEq(this.attrs, other.attrs);
  }
  range(from, to = from) {
    if (from >= to)
      throw new RangeError("Mark decorations may not be empty");
    return super.range(from, to);
  }
};
MarkDecoration.prototype.point = false;
var LineDecoration = class _LineDecoration extends Decoration {
  constructor(spec) {
    super(-2e8, -2e8, null, spec);
  }
  eq(other) {
    return other instanceof _LineDecoration && this.spec.class == other.spec.class && attrsEq(this.spec.attributes, other.spec.attributes);
  }
  range(from, to = from) {
    if (to != from)
      throw new RangeError("Line decoration ranges must be zero-length");
    return super.range(from, to);
  }
};
LineDecoration.prototype.mapMode = MapMode.TrackBefore;
LineDecoration.prototype.point = true;
var PointDecoration = class _PointDecoration extends Decoration {
  constructor(spec, startSide, endSide, block, widget, isReplace) {
    super(startSide, endSide, widget, spec);
    this.block = block;
    this.isReplace = isReplace;
    this.mapMode = !block ? MapMode.TrackDel : startSide <= 0 ? MapMode.TrackBefore : MapMode.TrackAfter;
  }
  // Only relevant when this.block == true
  get type() {
    return this.startSide != this.endSide ? BlockType.WidgetRange : this.startSide <= 0 ? BlockType.WidgetBefore : BlockType.WidgetAfter;
  }
  get heightRelevant() {
    return this.block || !!this.widget && (this.widget.estimatedHeight >= 5 || this.widget.lineBreaks > 0);
  }
  eq(other) {
    return other instanceof _PointDecoration && widgetsEq(this.widget, other.widget) && this.block == other.block && this.startSide == other.startSide && this.endSide == other.endSide;
  }
  range(from, to = from) {
    if (this.isReplace && (from > to || from == to && this.startSide > 0 && this.endSide <= 0))
      throw new RangeError("Invalid range for replacement decoration");
    if (!this.isReplace && to != from)
      throw new RangeError("Widget decorations can only have zero-length ranges");
    return super.range(from, to);
  }
};
PointDecoration.prototype.point = true;
function getInclusive(spec, block = false) {
  let { inclusiveStart: start, inclusiveEnd: end } = spec;
  if (start == null)
    start = spec.inclusive;
  if (end == null)
    end = spec.inclusive;
  return { start: start !== null && start !== void 0 ? start : block, end: end !== null && end !== void 0 ? end : block };
}
function widgetsEq(a2, b) {
  return a2 == b || !!(a2 && b && a2.compare(b));
}
function addRange(from, to, ranges, margin = 0) {
  let last = ranges.length - 1;
  if (last >= 0 && ranges[last] + margin >= from)
    ranges[last] = Math.max(ranges[last], to);
  else
    ranges.push(from, to);
}
var BlockWrapper = class _BlockWrapper extends RangeValue {
  constructor(tagName, attributes) {
    super();
    this.tagName = tagName;
    this.attributes = attributes;
  }
  eq(other) {
    return other == this || other instanceof _BlockWrapper && this.tagName == other.tagName && attrsEq(this.attributes, other.attributes);
  }
  /**
  Create a block wrapper object with the given tag name and
  attributes.
  */
  static create(spec) {
    return new _BlockWrapper(spec.tagName, spec.attributes || noAttrs);
  }
  /**
  Create a range set from the given block wrapper ranges.
  */
  static set(of, sort = false) {
    return RangeSet.of(of, sort);
  }
};
BlockWrapper.prototype.startSide = BlockWrapper.prototype.endSide = -1;
function getSelection(root) {
  let target;
  if (root.nodeType == 11) {
    target = root.getSelection ? root : root.ownerDocument;
  } else {
    target = root;
  }
  return target.getSelection();
}
function contains(dom, node) {
  return node ? dom == node || dom.contains(node.nodeType != 1 ? node.parentNode : node) : false;
}
function hasSelection(dom, selection) {
  if (!selection.anchorNode)
    return false;
  try {
    return contains(dom, selection.anchorNode);
  } catch (_) {
    return false;
  }
}
function clientRectsFor(dom) {
  if (dom.nodeType == 3)
    return textRange(dom, 0, dom.nodeValue.length).getClientRects();
  else if (dom.nodeType == 1)
    return dom.getClientRects();
  else
    return [];
}
function isEquivalentPosition(node, off, targetNode, targetOff) {
  return targetNode ? scanFor(node, off, targetNode, targetOff, -1) || scanFor(node, off, targetNode, targetOff, 1) : false;
}
function domIndex(node) {
  for (var index = 0; ; index++) {
    node = node.previousSibling;
    if (!node)
      return index;
  }
}
function isBlockElement(node) {
  return node.nodeType == 1 && /^(DIV|P|LI|UL|OL|BLOCKQUOTE|DD|DT|H\d|SECTION|PRE)$/.test(node.nodeName);
}
function scanFor(node, off, targetNode, targetOff, dir) {
  for (; ; ) {
    if (node == targetNode && off == targetOff)
      return true;
    if (off == (dir < 0 ? 0 : maxOffset(node))) {
      if (node.nodeName == "DIV")
        return false;
      let parent = node.parentNode;
      if (!parent || parent.nodeType != 1)
        return false;
      off = domIndex(node) + (dir < 0 ? 0 : 1);
      node = parent;
    } else if (node.nodeType == 1) {
      node = node.childNodes[off + (dir < 0 ? -1 : 0)];
      if (node.nodeType == 1 && node.contentEditable == "false")
        return false;
      off = dir < 0 ? maxOffset(node) : 0;
    } else {
      return false;
    }
  }
}
function maxOffset(node) {
  return node.nodeType == 3 ? node.nodeValue.length : node.childNodes.length;
}
function flattenRect(rect, left) {
  let x = left ? rect.left : rect.right;
  return { left: x, right: x, top: rect.top, bottom: rect.bottom };
}
function windowRect(win) {
  let vp = win.visualViewport;
  if (vp)
    return {
      left: 0,
      right: vp.width,
      top: 0,
      bottom: vp.height
    };
  return {
    left: 0,
    right: win.innerWidth,
    top: 0,
    bottom: win.innerHeight
  };
}
function getScale(elt, rect) {
  let scaleX = rect.width / elt.offsetWidth;
  let scaleY = rect.height / elt.offsetHeight;
  if (scaleX > 0.995 && scaleX < 1.005 || !isFinite(scaleX) || Math.abs(rect.width - elt.offsetWidth) < 1)
    scaleX = 1;
  if (scaleY > 0.995 && scaleY < 1.005 || !isFinite(scaleY) || Math.abs(rect.height - elt.offsetHeight) < 1)
    scaleY = 1;
  return { scaleX, scaleY };
}
function scrollRectIntoView(dom, rect, side, x, y, xMargin, yMargin, ltr) {
  let doc2 = dom.ownerDocument, win = doc2.defaultView || window;
  for (let cur = dom, stop = false; cur && !stop; ) {
    if (cur.nodeType == 1) {
      let bounding, top2 = cur == doc2.body;
      let scaleX = 1, scaleY = 1;
      if (top2) {
        bounding = windowRect(win);
      } else {
        if (/^(fixed|sticky)$/.test(getComputedStyle(cur).position))
          stop = true;
        if (cur.scrollHeight <= cur.clientHeight && cur.scrollWidth <= cur.clientWidth) {
          cur = cur.assignedSlot || cur.parentNode;
          continue;
        }
        let rect2 = cur.getBoundingClientRect();
        ({ scaleX, scaleY } = getScale(cur, rect2));
        bounding = {
          left: rect2.left,
          right: rect2.left + cur.clientWidth * scaleX,
          top: rect2.top,
          bottom: rect2.top + cur.clientHeight * scaleY
        };
      }
      let moveX = 0, moveY = 0;
      if (y == "nearest") {
        if (rect.top < bounding.top) {
          moveY = rect.top - (bounding.top + yMargin);
          if (side > 0 && rect.bottom > bounding.bottom + moveY)
            moveY = rect.bottom - bounding.bottom + yMargin;
        } else if (rect.bottom > bounding.bottom) {
          moveY = rect.bottom - bounding.bottom + yMargin;
          if (side < 0 && rect.top - moveY < bounding.top)
            moveY = rect.top - (bounding.top + yMargin);
        }
      } else {
        let rectHeight = rect.bottom - rect.top, boundingHeight = bounding.bottom - bounding.top;
        let targetTop = y == "center" && rectHeight <= boundingHeight ? rect.top + rectHeight / 2 - boundingHeight / 2 : y == "start" || y == "center" && side < 0 ? rect.top - yMargin : rect.bottom - boundingHeight + yMargin;
        moveY = targetTop - bounding.top;
      }
      if (x == "nearest") {
        if (rect.left < bounding.left) {
          moveX = rect.left - (bounding.left + xMargin);
          if (side > 0 && rect.right > bounding.right + moveX)
            moveX = rect.right - bounding.right + xMargin;
        } else if (rect.right > bounding.right) {
          moveX = rect.right - bounding.right + xMargin;
          if (side < 0 && rect.left < bounding.left + moveX)
            moveX = rect.left - (bounding.left + xMargin);
        }
      } else {
        let targetLeft = x == "center" ? rect.left + (rect.right - rect.left) / 2 - (bounding.right - bounding.left) / 2 : x == "start" == ltr ? rect.left - xMargin : rect.right - (bounding.right - bounding.left) + xMargin;
        moveX = targetLeft - bounding.left;
      }
      if (moveX || moveY) {
        if (top2) {
          win.scrollBy(moveX, moveY);
        } else {
          let movedX = 0, movedY = 0;
          if (moveY) {
            let start = cur.scrollTop;
            cur.scrollTop += moveY / scaleY;
            movedY = (cur.scrollTop - start) * scaleY;
          }
          if (moveX) {
            let start = cur.scrollLeft;
            cur.scrollLeft += moveX / scaleX;
            movedX = (cur.scrollLeft - start) * scaleX;
          }
          rect = {
            left: rect.left - movedX,
            top: rect.top - movedY,
            right: rect.right - movedX,
            bottom: rect.bottom - movedY
          };
          if (movedX && Math.abs(movedX - moveX) < 1)
            x = "nearest";
          if (movedY && Math.abs(movedY - moveY) < 1)
            y = "nearest";
        }
      }
      if (top2)
        break;
      if (rect.top < bounding.top || rect.bottom > bounding.bottom || rect.left < bounding.left || rect.right > bounding.right)
        rect = {
          left: Math.max(rect.left, bounding.left),
          right: Math.min(rect.right, bounding.right),
          top: Math.max(rect.top, bounding.top),
          bottom: Math.min(rect.bottom, bounding.bottom)
        };
      cur = cur.assignedSlot || cur.parentNode;
    } else if (cur.nodeType == 11) {
      cur = cur.host;
    } else {
      break;
    }
  }
}
function scrollableParents(dom) {
  let doc2 = dom.ownerDocument, x, y;
  for (let cur = dom.parentNode; cur; ) {
    if (cur == doc2.body || x && y) {
      break;
    } else if (cur.nodeType == 1) {
      if (!y && cur.scrollHeight > cur.clientHeight)
        y = cur;
      if (!x && cur.scrollWidth > cur.clientWidth)
        x = cur;
      cur = cur.assignedSlot || cur.parentNode;
    } else if (cur.nodeType == 11) {
      cur = cur.host;
    } else {
      break;
    }
  }
  return { x, y };
}
var DOMSelectionState = class {
  constructor() {
    this.anchorNode = null;
    this.anchorOffset = 0;
    this.focusNode = null;
    this.focusOffset = 0;
  }
  eq(domSel) {
    return this.anchorNode == domSel.anchorNode && this.anchorOffset == domSel.anchorOffset && this.focusNode == domSel.focusNode && this.focusOffset == domSel.focusOffset;
  }
  setRange(range) {
    let { anchorNode, focusNode } = range;
    this.set(anchorNode, Math.min(range.anchorOffset, anchorNode ? maxOffset(anchorNode) : 0), focusNode, Math.min(range.focusOffset, focusNode ? maxOffset(focusNode) : 0));
  }
  set(anchorNode, anchorOffset, focusNode, focusOffset) {
    this.anchorNode = anchorNode;
    this.anchorOffset = anchorOffset;
    this.focusNode = focusNode;
    this.focusOffset = focusOffset;
  }
};
var preventScrollSupported = null;
if (browser.safari && browser.safari_version >= 26)
  preventScrollSupported = false;
function focusPreventScroll(dom) {
  if (dom.setActive)
    return dom.setActive();
  if (preventScrollSupported)
    return dom.focus(preventScrollSupported);
  let stack = [];
  for (let cur = dom; cur; cur = cur.parentNode) {
    stack.push(cur, cur.scrollTop, cur.scrollLeft);
    if (cur == cur.ownerDocument)
      break;
  }
  dom.focus(preventScrollSupported == null ? {
    get preventScroll() {
      preventScrollSupported = { preventScroll: true };
      return true;
    }
  } : void 0);
  if (!preventScrollSupported) {
    preventScrollSupported = false;
    for (let i = 0; i < stack.length; ) {
      let elt = stack[i++], top2 = stack[i++], left = stack[i++];
      if (elt.scrollTop != top2)
        elt.scrollTop = top2;
      if (elt.scrollLeft != left)
        elt.scrollLeft = left;
    }
  }
}
var scratchRange;
function textRange(node, from, to = from) {
  let range = scratchRange || (scratchRange = document.createRange());
  range.setEnd(node, to);
  range.setStart(node, from);
  return range;
}
function dispatchKey(elt, name2, code, mods) {
  let options = { key: name2, code: name2, keyCode: code, which: code, cancelable: true };
  if (mods)
    ({ altKey: options.altKey, ctrlKey: options.ctrlKey, shiftKey: options.shiftKey, metaKey: options.metaKey } = mods);
  let down = new KeyboardEvent("keydown", options);
  down.synthetic = true;
  elt.dispatchEvent(down);
  let up = new KeyboardEvent("keyup", options);
  up.synthetic = true;
  elt.dispatchEvent(up);
  return down.defaultPrevented || up.defaultPrevented;
}
function getRoot(node) {
  while (node) {
    if (node && (node.nodeType == 9 || node.nodeType == 11 && node.host))
      return node;
    node = node.assignedSlot || node.parentNode;
  }
  return null;
}
function atElementStart(doc2, selection) {
  let node = selection.focusNode, offset = selection.focusOffset;
  if (!node || selection.anchorNode != node || selection.anchorOffset != offset)
    return false;
  offset = Math.min(offset, maxOffset(node));
  for (; ; ) {
    if (offset) {
      if (node.nodeType != 1)
        return false;
      let prev = node.childNodes[offset - 1];
      if (prev.contentEditable == "false")
        offset--;
      else {
        node = prev;
        offset = maxOffset(node);
      }
    } else if (node == doc2) {
      return true;
    } else {
      offset = domIndex(node);
      node = node.parentNode;
    }
  }
}
function isScrolledToBottom(elt) {
  return elt.scrollTop > Math.max(1, elt.scrollHeight - elt.clientHeight - 4);
}
function textNodeBefore(startNode, startOffset) {
  for (let node = startNode, offset = startOffset; ; ) {
    if (node.nodeType == 3 && offset > 0) {
      return { node, offset };
    } else if (node.nodeType == 1 && offset > 0) {
      if (node.contentEditable == "false")
        return null;
      node = node.childNodes[offset - 1];
      offset = maxOffset(node);
    } else if (node.parentNode && !isBlockElement(node)) {
      offset = domIndex(node);
      node = node.parentNode;
    } else {
      return null;
    }
  }
}
function textNodeAfter(startNode, startOffset) {
  for (let node = startNode, offset = startOffset; ; ) {
    if (node.nodeType == 3 && offset < node.nodeValue.length) {
      return { node, offset };
    } else if (node.nodeType == 1 && offset < node.childNodes.length) {
      if (node.contentEditable == "false")
        return null;
      node = node.childNodes[offset];
      offset = 0;
    } else if (node.parentNode && !isBlockElement(node)) {
      offset = domIndex(node) + 1;
      node = node.parentNode;
    } else {
      return null;
    }
  }
}
var DOMPos = class _DOMPos {
  constructor(node, offset, precise = true) {
    this.node = node;
    this.offset = offset;
    this.precise = precise;
  }
  static before(dom, precise) {
    return new _DOMPos(dom.parentNode, domIndex(dom), precise);
  }
  static after(dom, precise) {
    return new _DOMPos(dom.parentNode, domIndex(dom) + 1, precise);
  }
};
var Direction = /* @__PURE__ */ (function(Direction2) {
  Direction2[Direction2["LTR"] = 0] = "LTR";
  Direction2[Direction2["RTL"] = 1] = "RTL";
  return Direction2;
})(Direction || (Direction = {}));
var LTR = Direction.LTR;
var RTL = Direction.RTL;
function dec(str) {
  let result = [];
  for (let i = 0; i < str.length; i++)
    result.push(1 << +str[i]);
  return result;
}
var LowTypes = /* @__PURE__ */ dec("88888888888888888888888888888888888666888888787833333333337888888000000000000000000000000008888880000000000000000000000000088888888888888888888888888888888888887866668888088888663380888308888800000000000000000000000800000000000000000000000000000008");
var ArabicTypes = /* @__PURE__ */ dec("4444448826627288999999999992222222222222222222222222222222222222222222222229999999999999999999994444444444644222822222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222999999949999999229989999223333333333");
var Brackets = /* @__PURE__ */ Object.create(null);
var BracketStack = [];
for (let p of ["()", "[]", "{}"]) {
  let l = /* @__PURE__ */ p.charCodeAt(0), r = /* @__PURE__ */ p.charCodeAt(1);
  Brackets[l] = r;
  Brackets[r] = -l;
}
function charType(ch) {
  return ch <= 247 ? LowTypes[ch] : 1424 <= ch && ch <= 1524 ? 2 : 1536 <= ch && ch <= 1785 ? ArabicTypes[ch - 1536] : 1774 <= ch && ch <= 2220 ? 4 : 8192 <= ch && ch <= 8204 ? 256 : 64336 <= ch && ch <= 65023 ? 4 : 1;
}
var BidiRE = /[\u0590-\u05f4\u0600-\u06ff\u0700-\u08ac\ufb50-\ufdff]/;
var BidiSpan = class {
  /**
  The direction of this span.
  */
  get dir() {
    return this.level % 2 ? RTL : LTR;
  }
  /**
  @internal
  */
  constructor(from, to, level) {
    this.from = from;
    this.to = to;
    this.level = level;
  }
  /**
  @internal
  */
  side(end, dir) {
    return this.dir == dir == end ? this.to : this.from;
  }
  /**
  @internal
  */
  forward(forward, dir) {
    return forward == (this.dir == dir);
  }
  /**
  @internal
  */
  static find(order, index, level, assoc) {
    let maybe = -1;
    for (let i = 0; i < order.length; i++) {
      let span = order[i];
      if (span.from <= index && span.to >= index) {
        if (span.level == level)
          return i;
        if (maybe < 0 || (assoc != 0 ? assoc < 0 ? span.from < index : span.to > index : order[maybe].level > span.level))
          maybe = i;
      }
    }
    if (maybe < 0)
      throw new RangeError("Index out of range");
    return maybe;
  }
};
function isolatesEq(a2, b) {
  if (a2.length != b.length)
    return false;
  for (let i = 0; i < a2.length; i++) {
    let iA = a2[i], iB = b[i];
    if (iA.from != iB.from || iA.to != iB.to || iA.direction != iB.direction || !isolatesEq(iA.inner, iB.inner))
      return false;
  }
  return true;
}
var types = [];
function computeCharTypes(line, rFrom, rTo, isolates, outerType) {
  for (let iI = 0; iI <= isolates.length; iI++) {
    let from = iI ? isolates[iI - 1].to : rFrom, to = iI < isolates.length ? isolates[iI].from : rTo;
    let prevType = iI ? 256 : outerType;
    for (let i = from, prev = prevType, prevStrong = prevType; i < to; i++) {
      let type = charType(line.charCodeAt(i));
      if (type == 512)
        type = prev;
      else if (type == 8 && prevStrong == 4)
        type = 16;
      types[i] = type == 4 ? 2 : type;
      if (type & 7)
        prevStrong = type;
      prev = type;
    }
    for (let i = from, prev = prevType, prevStrong = prevType; i < to; i++) {
      let type = types[i];
      if (type == 128) {
        if (i < to - 1 && prev == types[i + 1] && prev & 24)
          type = types[i] = prev;
        else
          types[i] = 256;
      } else if (type == 64) {
        let end = i + 1;
        while (end < to && types[end] == 64)
          end++;
        let replace2 = i && prev == 8 || end < rTo && types[end] == 8 ? prevStrong == 1 ? 1 : 8 : 256;
        for (let j = i; j < end; j++)
          types[j] = replace2;
        i = end - 1;
      } else if (type == 8 && prevStrong == 1) {
        types[i] = 1;
      }
      prev = type;
      if (type & 7)
        prevStrong = type;
    }
  }
}
function processBracketPairs(line, rFrom, rTo, isolates, outerType) {
  let oppositeType = outerType == 1 ? 2 : 1;
  for (let iI = 0, sI = 0, context = 0; iI <= isolates.length; iI++) {
    let from = iI ? isolates[iI - 1].to : rFrom, to = iI < isolates.length ? isolates[iI].from : rTo;
    for (let i = from, ch, br, type; i < to; i++) {
      if (br = Brackets[ch = line.charCodeAt(i)]) {
        if (br < 0) {
          for (let sJ = sI - 3; sJ >= 0; sJ -= 3) {
            if (BracketStack[sJ + 1] == -br) {
              let flags = BracketStack[sJ + 2];
              let type2 = flags & 2 ? outerType : !(flags & 4) ? 0 : flags & 1 ? oppositeType : outerType;
              if (type2)
                types[i] = types[BracketStack[sJ]] = type2;
              sI = sJ;
              break;
            }
          }
        } else if (BracketStack.length == 189) {
          break;
        } else {
          BracketStack[sI++] = i;
          BracketStack[sI++] = ch;
          BracketStack[sI++] = context;
        }
      } else if ((type = types[i]) == 2 || type == 1) {
        let embed = type == outerType;
        context = embed ? 0 : 1;
        for (let sJ = sI - 3; sJ >= 0; sJ -= 3) {
          let cur = BracketStack[sJ + 2];
          if (cur & 2)
            break;
          if (embed) {
            BracketStack[sJ + 2] |= 2;
          } else {
            if (cur & 4)
              break;
            BracketStack[sJ + 2] |= 4;
          }
        }
      }
    }
  }
}
function processNeutrals(rFrom, rTo, isolates, outerType) {
  for (let iI = 0, prev = outerType; iI <= isolates.length; iI++) {
    let from = iI ? isolates[iI - 1].to : rFrom, to = iI < isolates.length ? isolates[iI].from : rTo;
    for (let i = from; i < to; ) {
      let type = types[i];
      if (type == 256) {
        let end = i + 1;
        for (; ; ) {
          if (end == to) {
            if (iI == isolates.length)
              break;
            end = isolates[iI++].to;
            to = iI < isolates.length ? isolates[iI].from : rTo;
          } else if (types[end] == 256) {
            end++;
          } else {
            break;
          }
        }
        let beforeL = prev == 1;
        let afterL = (end < rTo ? types[end] : outerType) == 1;
        let replace2 = beforeL == afterL ? beforeL ? 1 : 2 : outerType;
        for (let j = end, jI = iI, fromJ = jI ? isolates[jI - 1].to : rFrom; j > i; ) {
          if (j == fromJ) {
            j = isolates[--jI].from;
            fromJ = jI ? isolates[jI - 1].to : rFrom;
          }
          types[--j] = replace2;
        }
        i = end;
      } else {
        prev = type;
        i++;
      }
    }
  }
}
function emitSpans(line, from, to, level, baseLevel, isolates, order) {
  let ourType = level % 2 ? 2 : 1;
  if (level % 2 == baseLevel % 2) {
    for (let iCh = from, iI = 0; iCh < to; ) {
      let sameDir = true, isNum = false;
      if (iI == isolates.length || iCh < isolates[iI].from) {
        let next = types[iCh];
        if (next != ourType) {
          sameDir = false;
          isNum = next == 16;
        }
      }
      let recurse = !sameDir && ourType == 1 ? [] : null;
      let localLevel = sameDir ? level : level + 1;
      let iScan = iCh;
      run: for (; ; ) {
        if (iI < isolates.length && iScan == isolates[iI].from) {
          if (isNum)
            break run;
          let iso = isolates[iI];
          if (!sameDir)
            for (let upto = iso.to, jI = iI + 1; ; ) {
              if (upto == to)
                break run;
              if (jI < isolates.length && isolates[jI].from == upto)
                upto = isolates[jI++].to;
              else if (types[upto] == ourType)
                break run;
              else
                break;
            }
          iI++;
          if (recurse) {
            recurse.push(iso);
          } else {
            if (iso.from > iCh)
              order.push(new BidiSpan(iCh, iso.from, localLevel));
            let dirSwap = iso.direction == LTR != !(localLevel % 2);
            computeSectionOrder(line, dirSwap ? level + 1 : level, baseLevel, iso.inner, iso.from, iso.to, order);
            iCh = iso.to;
          }
          iScan = iso.to;
        } else if (iScan == to || (sameDir ? types[iScan] != ourType : types[iScan] == ourType)) {
          break;
        } else {
          iScan++;
        }
      }
      if (recurse)
        emitSpans(line, iCh, iScan, level + 1, baseLevel, recurse, order);
      else if (iCh < iScan)
        order.push(new BidiSpan(iCh, iScan, localLevel));
      iCh = iScan;
    }
  } else {
    for (let iCh = to, iI = isolates.length; iCh > from; ) {
      let sameDir = true, isNum = false;
      if (!iI || iCh > isolates[iI - 1].to) {
        let next = types[iCh - 1];
        if (next != ourType) {
          sameDir = false;
          isNum = next == 16;
        }
      }
      let recurse = !sameDir && ourType == 1 ? [] : null;
      let localLevel = sameDir ? level : level + 1;
      let iScan = iCh;
      run: for (; ; ) {
        if (iI && iScan == isolates[iI - 1].to) {
          if (isNum)
            break run;
          let iso = isolates[--iI];
          if (!sameDir)
            for (let upto = iso.from, jI = iI; ; ) {
              if (upto == from)
                break run;
              if (jI && isolates[jI - 1].to == upto)
                upto = isolates[--jI].from;
              else if (types[upto - 1] == ourType)
                break run;
              else
                break;
            }
          if (recurse) {
            recurse.push(iso);
          } else {
            if (iso.to < iCh)
              order.push(new BidiSpan(iso.to, iCh, localLevel));
            let dirSwap = iso.direction == LTR != !(localLevel % 2);
            computeSectionOrder(line, dirSwap ? level + 1 : level, baseLevel, iso.inner, iso.from, iso.to, order);
            iCh = iso.from;
          }
          iScan = iso.from;
        } else if (iScan == from || (sameDir ? types[iScan - 1] != ourType : types[iScan - 1] == ourType)) {
          break;
        } else {
          iScan--;
        }
      }
      if (recurse)
        emitSpans(line, iScan, iCh, level + 1, baseLevel, recurse, order);
      else if (iScan < iCh)
        order.push(new BidiSpan(iScan, iCh, localLevel));
      iCh = iScan;
    }
  }
}
function computeSectionOrder(line, level, baseLevel, isolates, from, to, order) {
  let outerType = level % 2 ? 2 : 1;
  computeCharTypes(line, from, to, isolates, outerType);
  processBracketPairs(line, from, to, isolates, outerType);
  processNeutrals(from, to, isolates, outerType);
  emitSpans(line, from, to, level, baseLevel, isolates, order);
}
function computeOrder(line, direction, isolates) {
  if (!line)
    return [new BidiSpan(0, 0, direction == RTL ? 1 : 0)];
  if (direction == LTR && !isolates.length && !BidiRE.test(line))
    return trivialOrder(line.length);
  if (isolates.length)
    while (line.length > types.length)
      types[types.length] = 256;
  let order = [], level = direction == LTR ? 0 : 1;
  computeSectionOrder(line, level, level, isolates, 0, line.length, order);
  return order;
}
function trivialOrder(length) {
  return [new BidiSpan(0, length, 0)];
}
var movedOver = "";
function moveVisually(line, order, dir, start, forward) {
  var _a2;
  let startIndex = start.head - line.from;
  let spanI = BidiSpan.find(order, startIndex, (_a2 = start.bidiLevel) !== null && _a2 !== void 0 ? _a2 : -1, start.assoc);
  let span = order[spanI], spanEnd = span.side(forward, dir);
  if (startIndex == spanEnd) {
    let nextI = spanI += forward ? 1 : -1;
    if (nextI < 0 || nextI >= order.length)
      return null;
    span = order[spanI = nextI];
    startIndex = span.side(!forward, dir);
    spanEnd = span.side(forward, dir);
  }
  let nextIndex = findClusterBreak2(line.text, startIndex, span.forward(forward, dir));
  if (nextIndex < span.from || nextIndex > span.to)
    nextIndex = spanEnd;
  movedOver = line.text.slice(Math.min(startIndex, nextIndex), Math.max(startIndex, nextIndex));
  let nextSpan = spanI == (forward ? order.length - 1 : 0) ? null : order[spanI + (forward ? 1 : -1)];
  if (nextSpan && nextIndex == spanEnd && nextSpan.level + (forward ? 0 : 1) < span.level)
    return EditorSelection.cursor(nextSpan.side(!forward, dir) + line.from, nextSpan.forward(forward, dir) ? 1 : -1, nextSpan.level);
  return EditorSelection.cursor(nextIndex + line.from, span.forward(forward, dir) ? -1 : 1, span.level);
}
function autoDirection(text, from, to) {
  for (let i = from; i < to; i++) {
    let type = charType(text.charCodeAt(i));
    if (type == 1)
      return LTR;
    if (type == 2 || type == 4)
      return RTL;
  }
  return LTR;
}
var clickAddsSelectionRange = /* @__PURE__ */ Facet.define();
var dragMovesSelection$1 = /* @__PURE__ */ Facet.define();
var mouseSelectionStyle = /* @__PURE__ */ Facet.define();
var exceptionSink = /* @__PURE__ */ Facet.define();
var updateListener = /* @__PURE__ */ Facet.define();
var inputHandler = /* @__PURE__ */ Facet.define();
var focusChangeEffect = /* @__PURE__ */ Facet.define();
var clipboardInputFilter = /* @__PURE__ */ Facet.define();
var clipboardOutputFilter = /* @__PURE__ */ Facet.define();
var perLineTextDirection = /* @__PURE__ */ Facet.define({
  combine: (values) => values.some((x) => x)
});
var nativeSelectionHidden = /* @__PURE__ */ Facet.define({
  combine: (values) => values.some((x) => x)
});
var scrollHandler = /* @__PURE__ */ Facet.define();
var ScrollTarget = class _ScrollTarget {
  constructor(range, y = "nearest", x = "nearest", yMargin = 5, xMargin = 5, isSnapshot = false) {
    this.range = range;
    this.y = y;
    this.x = x;
    this.yMargin = yMargin;
    this.xMargin = xMargin;
    this.isSnapshot = isSnapshot;
  }
  map(changes) {
    return changes.empty ? this : new _ScrollTarget(this.range.map(changes), this.y, this.x, this.yMargin, this.xMargin, this.isSnapshot);
  }
  clip(state) {
    return this.range.to <= state.doc.length ? this : new _ScrollTarget(EditorSelection.cursor(state.doc.length), this.y, this.x, this.yMargin, this.xMargin, this.isSnapshot);
  }
};
var scrollIntoView = /* @__PURE__ */ StateEffect.define({ map: (t2, ch) => t2.map(ch) });
var setEditContextFormatting = /* @__PURE__ */ StateEffect.define();
function logException(state, exception, context) {
  let handler = state.facet(exceptionSink);
  if (handler.length)
    handler[0](exception);
  else if (window.onerror && window.onerror(String(exception), context, void 0, void 0, exception)) ;
  else if (context)
    console.error(context + ":", exception);
  else
    console.error(exception);
}
var editable = /* @__PURE__ */ Facet.define({ combine: (values) => values.length ? values[0] : true });
var nextPluginID = 0;
var viewPlugin = /* @__PURE__ */ Facet.define({
  combine(plugins) {
    return plugins.filter((p, i) => {
      for (let j = 0; j < i; j++)
        if (plugins[j].plugin == p.plugin)
          return false;
      return true;
    });
  }
});
var ViewPlugin = class _ViewPlugin {
  constructor(id, create, domEventHandlers, domEventObservers, buildExtensions) {
    this.id = id;
    this.create = create;
    this.domEventHandlers = domEventHandlers;
    this.domEventObservers = domEventObservers;
    this.baseExtensions = buildExtensions(this);
    this.extension = this.baseExtensions.concat(viewPlugin.of({ plugin: this, arg: void 0 }));
  }
  /**
  Create an extension for this plugin with the given argument.
  */
  of(arg) {
    return this.baseExtensions.concat(viewPlugin.of({ plugin: this, arg }));
  }
  /**
  Define a plugin from a constructor function that creates the
  plugin's value, given an editor view.
  */
  static define(create, spec) {
    const { eventHandlers, eventObservers, provide, decorations: deco } = spec || {};
    return new _ViewPlugin(nextPluginID++, create, eventHandlers, eventObservers, (plugin) => {
      let ext = [];
      if (deco)
        ext.push(decorations.of((view) => {
          let pluginInst = view.plugin(plugin);
          return pluginInst ? deco(pluginInst) : Decoration.none;
        }));
      if (provide)
        ext.push(provide(plugin));
      return ext;
    });
  }
  /**
  Create a plugin for a class whose constructor takes a single
  editor view as argument.
  */
  static fromClass(cls, spec) {
    return _ViewPlugin.define((view, arg) => new cls(view, arg), spec);
  }
};
var PluginInstance = class {
  constructor(spec) {
    this.spec = spec;
    this.mustUpdate = null;
    this.value = null;
  }
  get plugin() {
    return this.spec && this.spec.plugin;
  }
  update(view) {
    if (!this.value) {
      if (this.spec) {
        try {
          this.value = this.spec.plugin.create(view, this.spec.arg);
        } catch (e) {
          logException(view.state, e, "CodeMirror plugin crashed");
          this.deactivate();
        }
      }
    } else if (this.mustUpdate) {
      let update = this.mustUpdate;
      this.mustUpdate = null;
      if (this.value.update) {
        try {
          this.value.update(update);
        } catch (e) {
          logException(update.state, e, "CodeMirror plugin crashed");
          if (this.value.destroy)
            try {
              this.value.destroy();
            } catch (_) {
            }
          this.deactivate();
        }
      }
    }
    return this;
  }
  destroy(view) {
    var _a2;
    if ((_a2 = this.value) === null || _a2 === void 0 ? void 0 : _a2.destroy) {
      try {
        this.value.destroy();
      } catch (e) {
        logException(view.state, e, "CodeMirror plugin crashed");
      }
    }
  }
  deactivate() {
    this.spec = this.value = null;
  }
};
var editorAttributes = /* @__PURE__ */ Facet.define();
var contentAttributes = /* @__PURE__ */ Facet.define();
var decorations = /* @__PURE__ */ Facet.define();
var blockWrappers = /* @__PURE__ */ Facet.define();
var outerDecorations = /* @__PURE__ */ Facet.define();
var atomicRanges = /* @__PURE__ */ Facet.define();
var bidiIsolatedRanges = /* @__PURE__ */ Facet.define();
function getIsolatedRanges(view, line) {
  let isolates = view.state.facet(bidiIsolatedRanges);
  if (!isolates.length)
    return isolates;
  let sets = isolates.map((i) => i instanceof Function ? i(view) : i);
  let result = [];
  RangeSet.spans(sets, line.from, line.to, {
    point() {
    },
    span(fromDoc, toDoc, active, open) {
      let from = fromDoc - line.from, to = toDoc - line.from;
      let level = result;
      for (let i = active.length - 1; i >= 0; i--, open--) {
        let direction = active[i].spec.bidiIsolate, update;
        if (direction == null)
          direction = autoDirection(line.text, from, to);
        if (open > 0 && level.length && (update = level[level.length - 1]).to == from && update.direction == direction) {
          update.to = to;
          level = update.inner;
        } else {
          let add = { from, to, direction, inner: [] };
          level.push(add);
          level = add.inner;
        }
      }
    }
  });
  return result;
}
var scrollMargins = /* @__PURE__ */ Facet.define();
function getScrollMargins(view) {
  let left = 0, right = 0, top2 = 0, bottom = 0;
  for (let source of view.state.facet(scrollMargins)) {
    let m = source(view);
    if (m) {
      if (m.left != null)
        left = Math.max(left, m.left);
      if (m.right != null)
        right = Math.max(right, m.right);
      if (m.top != null)
        top2 = Math.max(top2, m.top);
      if (m.bottom != null)
        bottom = Math.max(bottom, m.bottom);
    }
  }
  return { left, right, top: top2, bottom };
}
var styleModule = /* @__PURE__ */ Facet.define();
var ChangedRange = class _ChangedRange {
  constructor(fromA, toA, fromB, toB) {
    this.fromA = fromA;
    this.toA = toA;
    this.fromB = fromB;
    this.toB = toB;
  }
  join(other) {
    return new _ChangedRange(Math.min(this.fromA, other.fromA), Math.max(this.toA, other.toA), Math.min(this.fromB, other.fromB), Math.max(this.toB, other.toB));
  }
  addToSet(set) {
    let i = set.length, me = this;
    for (; i > 0; i--) {
      let range = set[i - 1];
      if (range.fromA > me.toA)
        continue;
      if (range.toA < me.fromA)
        break;
      me = me.join(range);
      set.splice(i - 1, 1);
    }
    set.splice(i, 0, me);
    return set;
  }
  // Extend a set to cover all the content in `ranges`, which is a
  // flat array with each pair of numbers representing fromB/toB
  // positions. These pairs are generated in unchanged ranges, so the
  // offset between doc A and doc B is the same for their start and
  // end points.
  static extendWithRanges(diff, ranges) {
    if (ranges.length == 0)
      return diff;
    let result = [];
    for (let dI = 0, rI = 0, off = 0; ; ) {
      let nextD = dI < diff.length ? diff[dI].fromB : 1e9;
      let nextR = rI < ranges.length ? ranges[rI] : 1e9;
      let fromB = Math.min(nextD, nextR);
      if (fromB == 1e9)
        break;
      let fromA = fromB + off, toB = fromB, toA = fromA;
      for (; ; ) {
        if (rI < ranges.length && ranges[rI] <= toB) {
          let end = ranges[rI + 1];
          rI += 2;
          toB = Math.max(toB, end);
          for (let i = dI; i < diff.length && diff[i].fromB <= toB; i++)
            off = diff[i].toA - diff[i].toB;
          toA = Math.max(toA, end + off);
        } else if (dI < diff.length && diff[dI].fromB <= toB) {
          let next = diff[dI++];
          toB = Math.max(toB, next.toB);
          toA = Math.max(toA, next.toA);
          off = next.toA - next.toB;
        } else {
          break;
        }
      }
      result.push(new _ChangedRange(fromA, toA, fromB, toB));
    }
    return result;
  }
};
var ViewUpdate = class _ViewUpdate {
  constructor(view, state, transactions) {
    this.view = view;
    this.state = state;
    this.transactions = transactions;
    this.flags = 0;
    this.startState = view.state;
    this.changes = ChangeSet.empty(this.startState.doc.length);
    for (let tr of transactions)
      this.changes = this.changes.compose(tr.changes);
    let changedRanges = [];
    this.changes.iterChangedRanges((fromA, toA, fromB, toB) => changedRanges.push(new ChangedRange(fromA, toA, fromB, toB)));
    this.changedRanges = changedRanges;
  }
  /**
  @internal
  */
  static create(view, state, transactions) {
    return new _ViewUpdate(view, state, transactions);
  }
  /**
  Tells you whether the [viewport](https://codemirror.net/6/docs/ref/#view.EditorView.viewport) or
  [visible ranges](https://codemirror.net/6/docs/ref/#view.EditorView.visibleRanges) changed in this
  update.
  */
  get viewportChanged() {
    return (this.flags & 4) > 0;
  }
  /**
  Returns true when
  [`viewportChanged`](https://codemirror.net/6/docs/ref/#view.ViewUpdate.viewportChanged) is true
  and the viewport change is not just the result of mapping it in
  response to document changes.
  */
  get viewportMoved() {
    return (this.flags & 8) > 0;
  }
  /**
  Indicates whether the height of a block element in the editor
  changed in this update.
  */
  get heightChanged() {
    return (this.flags & 2) > 0;
  }
  /**
  Returns true when the document was modified or the size of the
  editor, or elements within the editor, changed.
  */
  get geometryChanged() {
    return this.docChanged || (this.flags & (16 | 2)) > 0;
  }
  /**
  True when this update indicates a focus change.
  */
  get focusChanged() {
    return (this.flags & 1) > 0;
  }
  /**
  Whether the document changed in this update.
  */
  get docChanged() {
    return !this.changes.empty;
  }
  /**
  Whether the selection was explicitly set in this update.
  */
  get selectionSet() {
    return this.transactions.some((tr) => tr.selection);
  }
  /**
  @internal
  */
  get empty() {
    return this.flags == 0 && this.transactions.length == 0;
  }
};
var noChildren = [];
var Tile = class {
  constructor(dom, length, flags = 0) {
    this.dom = dom;
    this.length = length;
    this.flags = flags;
    this.parent = null;
    dom.cmTile = this;
  }
  get breakAfter() {
    return this.flags & 1;
  }
  get children() {
    return noChildren;
  }
  isWidget() {
    return false;
  }
  get isHidden() {
    return false;
  }
  isComposite() {
    return false;
  }
  isLine() {
    return false;
  }
  isText() {
    return false;
  }
  isBlock() {
    return false;
  }
  get domAttrs() {
    return null;
  }
  sync() {
    this.flags |= 2;
    if (this.flags & 4) {
      this.flags &= ~4;
      let attrs = this.domAttrs;
      if (attrs)
        setAttrs(this.dom, attrs);
    }
  }
  toString() {
    return this.constructor.name + (this.children.length ? `(${this.children})` : "") + (this.breakAfter ? "#" : "");
  }
  destroy() {
    this.parent = null;
  }
  setDOM(dom) {
    this.dom = dom;
    dom.cmTile = this;
  }
  get posAtStart() {
    return this.parent ? this.parent.posBefore(this) : 0;
  }
  get posAtEnd() {
    return this.posAtStart + this.length;
  }
  posBefore(tile, start = this.posAtStart) {
    let pos = start;
    for (let child of this.children) {
      if (child == tile)
        return pos;
      pos += child.length + child.breakAfter;
    }
    throw new RangeError("Invalid child in posBefore");
  }
  posAfter(tile) {
    return this.posBefore(tile) + tile.length;
  }
  covers(side) {
    return true;
  }
  coordsIn(pos, side) {
    return null;
  }
  domPosFor(off, side) {
    let index = domIndex(this.dom);
    let after = this.length ? off > 0 : side > 0;
    return new DOMPos(this.parent.dom, index + (after ? 1 : 0), off == 0 || off == this.length);
  }
  markDirty(attrs) {
    this.flags &= ~2;
    if (attrs)
      this.flags |= 4;
    if (this.parent && this.parent.flags & 2)
      this.parent.markDirty(false);
  }
  get overrideDOMText() {
    return null;
  }
  get root() {
    for (let t2 = this; t2; t2 = t2.parent)
      if (t2 instanceof DocTile)
        return t2;
    return null;
  }
  static get(dom) {
    return dom.cmTile;
  }
};
var CompositeTile = class extends Tile {
  constructor(dom) {
    super(dom, 0);
    this._children = [];
  }
  isComposite() {
    return true;
  }
  get children() {
    return this._children;
  }
  get lastChild() {
    return this.children.length ? this.children[this.children.length - 1] : null;
  }
  append(child) {
    this.children.push(child);
    child.parent = this;
  }
  sync() {
    if (this.flags & 2)
      return;
    super.sync();
    let parent = this.dom, prev = null, next;
    let length = 0;
    for (let child of this.children) {
      child.sync();
      length += child.length + child.breakAfter;
      next = prev ? prev.nextSibling : parent.firstChild;
      if (child.dom.parentNode == parent) {
        while (next && next != child.dom)
          next = rm$1(next);
      } else {
        parent.insertBefore(child.dom, next);
      }
      prev = child.dom;
    }
    next = prev ? prev.nextSibling : parent.firstChild;
    while (next)
      next = rm$1(next);
    this.length = length;
  }
};
function rm$1(dom) {
  let next = dom.nextSibling;
  dom.parentNode.removeChild(dom);
  return next;
}
var DocTile = class extends CompositeTile {
  constructor(view, dom) {
    super(dom);
    this.view = view;
  }
  owns(tile) {
    for (; tile; tile = tile.parent)
      if (tile == this)
        return true;
    return false;
  }
  isBlock() {
    return true;
  }
  nearest(dom) {
    for (; ; ) {
      if (!dom)
        return null;
      let tile = Tile.get(dom);
      if (tile && this.owns(tile))
        return tile;
      dom = dom.parentNode;
    }
  }
  blockTiles(f) {
    for (let stack = [], cur = this, i = 0, pos = 0; ; ) {
      if (i == cur.children.length) {
        if (!stack.length)
          return;
        cur = cur.parent;
        if (cur.breakAfter)
          pos++;
        i = stack.pop();
      } else {
        let next = cur.children[i++];
        if (next instanceof BlockWrapperTile) {
          stack.push(i);
          cur = next;
          i = 0;
        } else {
          let end = pos + next.length;
          let result = f(next, pos);
          if (result !== void 0)
            return result;
          pos = end + next.breakAfter;
        }
      }
    }
  }
  // Find the block at the given position. If side < -1, make sure to
  // stay before block widgets at that position, if side > 1, after
  // such widgets (used for selection drawing, which needs to be able
  // to get coordinates for positions that aren't valid cursor positions).
  resolveBlock(pos, side) {
    let before, beforeOff = -1, after, afterOff = -1;
    this.blockTiles((tile, off) => {
      let end = off + tile.length;
      if (pos >= off && pos <= end) {
        if (tile.isWidget() && side >= -1 && side <= 1) {
          if (tile.flags & 32)
            return true;
          if (tile.flags & 16)
            before = void 0;
        }
        if ((off < pos || pos == end && (side < -1 ? tile.length : tile.covers(1))) && (!before || !tile.isWidget() && before.isWidget())) {
          before = tile;
          beforeOff = pos - off;
        }
        if ((end > pos || pos == off && (side > 1 ? tile.length : tile.covers(-1))) && (!after || !tile.isWidget() && after.isWidget())) {
          after = tile;
          afterOff = pos - off;
        }
      }
    });
    if (!before && !after)
      throw new Error("No tile at position " + pos);
    return before && side < 0 || !after ? { tile: before, offset: beforeOff } : { tile: after, offset: afterOff };
  }
};
var BlockWrapperTile = class _BlockWrapperTile extends CompositeTile {
  constructor(dom, wrapper) {
    super(dom);
    this.wrapper = wrapper;
  }
  isBlock() {
    return true;
  }
  covers(side) {
    if (!this.children.length)
      return false;
    return side < 0 ? this.children[0].covers(-1) : this.lastChild.covers(1);
  }
  get domAttrs() {
    return this.wrapper.attributes;
  }
  static of(wrapper, dom) {
    let tile = new _BlockWrapperTile(dom || document.createElement(wrapper.tagName), wrapper);
    if (!dom)
      tile.flags |= 4;
    return tile;
  }
};
var LineTile = class _LineTile extends CompositeTile {
  constructor(dom, attrs) {
    super(dom);
    this.attrs = attrs;
  }
  isLine() {
    return true;
  }
  static start(attrs, dom, keepAttrs) {
    let line = new _LineTile(dom || document.createElement("div"), attrs);
    if (!dom || !keepAttrs)
      line.flags |= 4;
    return line;
  }
  get domAttrs() {
    return this.attrs;
  }
  // Find the tile associated with a given position in this line.
  resolveInline(pos, side, forCoords) {
    let before = null, beforeOff = -1, after = null, afterOff = -1;
    function scan(tile, pos2) {
      for (let i = 0, off = 0; i < tile.children.length && off <= pos2; i++) {
        let child = tile.children[i], end = off + child.length;
        if (end >= pos2) {
          if (child.isComposite()) {
            scan(child, pos2 - off);
          } else if ((!after || after.isHidden && (side > 0 || forCoords && onSameLine(after, child))) && (end > pos2 || child.flags & 32)) {
            after = child;
            afterOff = pos2 - off;
          } else if (off < pos2 || child.flags & 16 && !child.isHidden) {
            before = child;
            beforeOff = pos2 - off;
          }
        }
        off = end;
      }
    }
    scan(this, pos);
    let target = (side < 0 ? before : after) || before || after;
    return target ? { tile: target, offset: target == before ? beforeOff : afterOff } : null;
  }
  coordsIn(pos, side) {
    let found = this.resolveInline(pos, side, true);
    if (!found)
      return fallbackRect(this);
    return found.tile.coordsIn(Math.max(0, found.offset), side);
  }
  domIn(pos, side) {
    let found = this.resolveInline(pos, side);
    if (found) {
      let { tile, offset } = found;
      if (this.dom.contains(tile.dom)) {
        if (tile.isText())
          return new DOMPos(tile.dom, Math.min(tile.dom.nodeValue.length, offset));
        return tile.domPosFor(offset, side);
      }
      let parent = found.tile.parent, saw = false;
      for (let ch of parent.children) {
        if (saw)
          return new DOMPos(ch.dom, 0);
        if (ch == found.tile) {
          saw = true;
        }
      }
    }
    return new DOMPos(this.dom, 0);
  }
};
function fallbackRect(tile) {
  let last = tile.dom.lastChild;
  if (!last)
    return tile.dom.getBoundingClientRect();
  let rects = clientRectsFor(last);
  return rects[rects.length - 1] || null;
}
function onSameLine(a2, b) {
  let posA = a2.coordsIn(0, 1), posB = b.coordsIn(0, 1);
  return posA && posB && posB.top < posA.bottom;
}
var MarkTile = class _MarkTile extends CompositeTile {
  constructor(dom, mark) {
    super(dom);
    this.mark = mark;
  }
  get domAttrs() {
    return this.mark.attrs;
  }
  static of(mark, dom) {
    let tile = new _MarkTile(dom || document.createElement(mark.tagName), mark);
    if (!dom)
      tile.flags |= 4;
    return tile;
  }
};
var TextTile = class _TextTile extends Tile {
  constructor(dom, text) {
    super(dom, text.length);
    this.text = text;
  }
  sync() {
    if (this.flags & 2)
      return;
    super.sync();
    if (this.dom.nodeValue != this.text)
      this.dom.nodeValue = this.text;
  }
  isText() {
    return true;
  }
  toString() {
    return JSON.stringify(this.text);
  }
  coordsIn(pos, side) {
    let length = this.dom.nodeValue.length;
    if (pos > length)
      pos = length;
    let from = pos, to = pos, flatten2 = 0;
    if (pos == 0 && side < 0 || pos == length && side >= 0) {
      if (!(browser.chrome || browser.gecko)) {
        if (pos) {
          from--;
          flatten2 = 1;
        } else if (to < length) {
          to++;
          flatten2 = -1;
        }
      }
    } else {
      if (side < 0)
        from--;
      else if (to < length)
        to++;
    }
    let rects = textRange(this.dom, from, to).getClientRects();
    if (!rects.length)
      return null;
    let rect = rects[(flatten2 ? flatten2 < 0 : side >= 0) ? 0 : rects.length - 1];
    if (browser.safari && !flatten2 && rect.width == 0)
      rect = Array.prototype.find.call(rects, (r) => r.width) || rect;
    return flatten2 ? flattenRect(rect, flatten2 < 0) : rect || null;
  }
  static of(text, dom) {
    let tile = new _TextTile(dom || document.createTextNode(text), text);
    if (!dom)
      tile.flags |= 2;
    return tile;
  }
};
var WidgetTile = class _WidgetTile extends Tile {
  constructor(dom, length, widget, flags) {
    super(dom, length, flags);
    this.widget = widget;
  }
  isWidget() {
    return true;
  }
  get isHidden() {
    return this.widget.isHidden;
  }
  covers(side) {
    if (this.flags & 48)
      return false;
    return (this.flags & (side < 0 ? 64 : 128)) > 0;
  }
  coordsIn(pos, side) {
    return this.coordsInWidget(pos, side, false);
  }
  coordsInWidget(pos, side, block) {
    let custom = this.widget.coordsAt(this.dom, pos, side);
    if (custom)
      return custom;
    if (block) {
      return flattenRect(this.dom.getBoundingClientRect(), this.length ? pos == 0 : side <= 0);
    } else {
      let rects = this.dom.getClientRects(), rect = null;
      if (!rects.length)
        return null;
      let fromBack = this.flags & 16 ? true : this.flags & 32 ? false : pos > 0;
      for (let i = fromBack ? rects.length - 1 : 0; ; i += fromBack ? -1 : 1) {
        rect = rects[i];
        if (pos > 0 ? i == 0 : i == rects.length - 1 || rect.top < rect.bottom)
          break;
      }
      return flattenRect(rect, !fromBack);
    }
  }
  get overrideDOMText() {
    if (!this.length)
      return Text.empty;
    let { root } = this;
    if (!root)
      return Text.empty;
    let start = this.posAtStart;
    return root.view.state.doc.slice(start, start + this.length);
  }
  destroy() {
    super.destroy();
    this.widget.destroy(this.dom);
  }
  static of(widget, view, length, flags, dom) {
    if (!dom) {
      dom = widget.toDOM(view);
      if (!widget.editable)
        dom.contentEditable = "false";
    }
    return new _WidgetTile(dom, length, widget, flags);
  }
};
var WidgetBufferTile = class extends Tile {
  constructor(flags) {
    let img = document.createElement("img");
    img.className = "cm-widgetBuffer";
    img.setAttribute("aria-hidden", "true");
    super(img, 0, flags);
  }
  get isHidden() {
    return false;
  }
  get overrideDOMText() {
    return Text.empty;
  }
  coordsIn(pos) {
    return this.dom.getBoundingClientRect();
  }
};
var TilePointer = class {
  constructor(top2) {
    this.index = 0;
    this.beforeBreak = false;
    this.parents = [];
    this.tile = top2;
  }
  // Advance by the given distance. If side is -1, stop leaving or
  // entering tiles, or skipping zero-length tiles, once the distance
  // has been traversed. When side is 1, leave, enter, or skip
  // everything at the end position.
  advance(dist2, side, walker) {
    let { tile, index, beforeBreak, parents } = this;
    while (dist2 || side > 0) {
      if (!tile.isComposite()) {
        if (index == tile.length) {
          beforeBreak = !!tile.breakAfter;
          ({ tile, index } = parents.pop());
          index++;
        } else if (!dist2) {
          break;
        } else {
          let take = Math.min(dist2, tile.length - index);
          if (walker)
            walker.skip(tile, index, index + take);
          dist2 -= take;
          index += take;
        }
      } else if (beforeBreak) {
        if (!dist2)
          break;
        if (walker)
          walker.break();
        dist2--;
        beforeBreak = false;
      } else if (index == tile.children.length) {
        if (!dist2 && !parents.length)
          break;
        if (walker)
          walker.leave(tile);
        beforeBreak = !!tile.breakAfter;
        ({ tile, index } = parents.pop());
        index++;
      } else {
        let next = tile.children[index], brk = next.breakAfter;
        if ((side > 0 ? next.length <= dist2 : next.length < dist2) && (!walker || walker.skip(next, 0, next.length) !== false || !next.isComposite)) {
          beforeBreak = !!brk;
          index++;
          dist2 -= next.length;
        } else {
          parents.push({ tile, index });
          tile = next;
          index = 0;
          if (walker && next.isComposite())
            walker.enter(next);
        }
      }
    }
    this.tile = tile;
    this.index = index;
    this.beforeBreak = beforeBreak;
    return this;
  }
  get root() {
    return this.parents.length ? this.parents[0].tile : this.tile;
  }
};
var OpenWrapper = class {
  constructor(from, to, wrapper, rank) {
    this.from = from;
    this.to = to;
    this.wrapper = wrapper;
    this.rank = rank;
  }
};
var TileBuilder = class {
  constructor(cache, root, blockWrappers2) {
    this.cache = cache;
    this.root = root;
    this.blockWrappers = blockWrappers2;
    this.curLine = null;
    this.lastBlock = null;
    this.afterWidget = null;
    this.pos = 0;
    this.wrappers = [];
    this.wrapperPos = 0;
  }
  addText(text, marks2, openStart, tile) {
    var _a2;
    this.flushBuffer();
    let parent = this.ensureMarks(marks2, openStart);
    let prev = parent.lastChild;
    if (prev && prev.isText() && !(prev.flags & 8)) {
      this.cache.reused.set(
        prev,
        2
        /* Reused.DOM */
      );
      let tile2 = parent.children[parent.children.length - 1] = new TextTile(prev.dom, prev.text + text);
      tile2.parent = parent;
    } else {
      parent.append(tile || TextTile.of(text, (_a2 = this.cache.find(TextTile)) === null || _a2 === void 0 ? void 0 : _a2.dom));
    }
    this.pos += text.length;
    this.afterWidget = null;
  }
  addComposition(composition, context) {
    let line = this.curLine;
    if (line.dom != context.line.dom) {
      line.setDOM(this.cache.reused.has(context.line) ? freeNode(context.line.dom) : context.line.dom);
      this.cache.reused.set(
        context.line,
        2
        /* Reused.DOM */
      );
    }
    let head = line;
    for (let i = context.marks.length - 1; i >= 0; i--) {
      let mark = context.marks[i];
      let last = head.lastChild;
      if (last instanceof MarkTile && last.mark.eq(mark.mark)) {
        if (last.dom != mark.dom)
          last.setDOM(freeNode(mark.dom));
        head = last;
      } else {
        if (this.cache.reused.get(mark)) {
          let tile = Tile.get(mark.dom);
          if (tile)
            tile.setDOM(freeNode(mark.dom));
        }
        let nw = MarkTile.of(mark.mark, mark.dom);
        head.append(nw);
        head = nw;
      }
      this.cache.reused.set(
        mark,
        2
        /* Reused.DOM */
      );
    }
    let text = new TextTile(composition.text, composition.text.nodeValue);
    text.flags |= 8;
    head.append(text);
  }
  addInlineWidget(widget, marks2, openStart) {
    let noSpace = this.afterWidget && widget.flags & 48 && (this.afterWidget.flags & 48) == (widget.flags & 48);
    if (!noSpace)
      this.flushBuffer();
    let parent = this.ensureMarks(marks2, openStart);
    if (!noSpace && !(widget.flags & 16))
      parent.append(this.getBuffer(1));
    parent.append(widget);
    this.pos += widget.length;
    this.afterWidget = widget;
  }
  addMark(tile, marks2, openStart) {
    this.flushBuffer();
    let parent = this.ensureMarks(marks2, openStart);
    parent.append(tile);
    this.pos += tile.length;
    this.afterWidget = null;
  }
  addBlockWidget(widget) {
    this.getBlockPos().append(widget);
    this.pos += widget.length;
    this.lastBlock = widget;
    this.endLine();
  }
  continueWidget(length) {
    let widget = this.afterWidget || this.lastBlock;
    widget.length += length;
    this.pos += length;
  }
  addLineStart(attrs, dom) {
    var _a2;
    if (!attrs)
      attrs = lineBaseAttrs;
    let tile = LineTile.start(attrs, dom || ((_a2 = this.cache.find(LineTile)) === null || _a2 === void 0 ? void 0 : _a2.dom), !!dom);
    this.getBlockPos().append(this.lastBlock = this.curLine = tile);
  }
  addLine(tile) {
    this.getBlockPos().append(tile);
    this.pos += tile.length;
    this.lastBlock = tile;
    this.endLine();
  }
  addBreak() {
    this.lastBlock.flags |= 1;
    this.endLine();
    this.pos++;
  }
  addLineStartIfNotCovered(attrs) {
    if (!this.blockPosCovered())
      this.addLineStart(attrs);
  }
  ensureLine(attrs) {
    if (!this.curLine)
      this.addLineStart(attrs);
  }
  ensureMarks(marks2, openStart) {
    var _a2;
    let parent = this.curLine;
    for (let i = marks2.length - 1; i >= 0; i--) {
      let mark = marks2[i], last;
      if (openStart > 0 && (last = parent.lastChild) && last instanceof MarkTile && last.mark.eq(mark)) {
        parent = last;
        openStart--;
      } else {
        let tile = MarkTile.of(mark, (_a2 = this.cache.find(MarkTile, (m) => m.mark.eq(mark))) === null || _a2 === void 0 ? void 0 : _a2.dom);
        parent.append(tile);
        parent = tile;
        openStart = 0;
      }
    }
    return parent;
  }
  endLine() {
    if (this.curLine) {
      this.flushBuffer();
      let last = this.curLine.lastChild;
      if (!last || !hasContent(this.curLine, false) || last.dom.nodeName != "BR" && last.isWidget() && !(browser.ios && hasContent(this.curLine, true)))
        this.curLine.append(this.cache.findWidget(
          BreakWidget,
          0,
          32
          /* TileFlag.After */
        ) || new WidgetTile(
          BreakWidget.toDOM(),
          0,
          BreakWidget,
          32
          /* TileFlag.After */
        ));
      this.curLine = this.afterWidget = null;
    }
  }
  updateBlockWrappers() {
    if (this.wrapperPos > this.pos + 1e4) {
      this.blockWrappers.goto(this.pos);
      this.wrappers.length = 0;
    }
    for (let i = this.wrappers.length - 1; i >= 0; i--)
      if (this.wrappers[i].to < this.pos)
        this.wrappers.splice(i, 1);
    for (let cur = this.blockWrappers; cur.value && cur.from <= this.pos; cur.next())
      if (cur.to >= this.pos) {
        let wrap = new OpenWrapper(cur.from, cur.to, cur.value, cur.rank), i = this.wrappers.length;
        while (i > 0 && (this.wrappers[i - 1].rank - wrap.rank || this.wrappers[i - 1].to - wrap.to) < 0)
          i--;
        this.wrappers.splice(i, 0, wrap);
      }
    this.wrapperPos = this.pos;
  }
  getBlockPos() {
    var _a2;
    this.updateBlockWrappers();
    let parent = this.root;
    for (let wrap of this.wrappers) {
      let last = parent.lastChild;
      if (wrap.from < this.pos && last instanceof BlockWrapperTile && last.wrapper.eq(wrap.wrapper)) {
        parent = last;
      } else {
        let tile = BlockWrapperTile.of(wrap.wrapper, (_a2 = this.cache.find(BlockWrapperTile, (t2) => t2.wrapper.eq(wrap.wrapper))) === null || _a2 === void 0 ? void 0 : _a2.dom);
        parent.append(tile);
        parent = tile;
      }
    }
    return parent;
  }
  blockPosCovered() {
    let last = this.lastBlock;
    return last != null && !last.breakAfter && (!last.isWidget() || (last.flags & (32 | 128)) > 0);
  }
  getBuffer(side) {
    let flags = 2 | (side < 0 ? 16 : 32);
    let found = this.cache.find(
      WidgetBufferTile,
      void 0,
      1
      /* Reused.Full */
    );
    if (found)
      found.flags = flags;
    return found || new WidgetBufferTile(flags);
  }
  flushBuffer() {
    if (this.afterWidget && !(this.afterWidget.flags & 32)) {
      this.afterWidget.parent.append(this.getBuffer(-1));
      this.afterWidget = null;
    }
  }
};
var TextStream = class {
  constructor(doc2) {
    this.skipCount = 0;
    this.text = "";
    this.textOff = 0;
    this.cursor = doc2.iter();
  }
  skip(len) {
    if (this.textOff + len <= this.text.length) {
      this.textOff += len;
    } else {
      this.skipCount += len - (this.text.length - this.textOff);
      this.text = "";
      this.textOff = 0;
    }
  }
  next(maxLen) {
    if (this.textOff == this.text.length) {
      let { value, lineBreak, done } = this.cursor.next(this.skipCount);
      this.skipCount = 0;
      if (done)
        throw new Error("Ran out of text content when drawing inline views");
      this.text = value;
      let len = this.textOff = Math.min(maxLen, value.length);
      return lineBreak ? null : value.slice(0, len);
    }
    let end = Math.min(this.text.length, this.textOff + maxLen);
    let chars = this.text.slice(this.textOff, end);
    this.textOff = end;
    return chars;
  }
};
var buckets = [WidgetTile, LineTile, TextTile, MarkTile, WidgetBufferTile, BlockWrapperTile, DocTile];
for (let i = 0; i < buckets.length; i++)
  buckets[i].bucket = i;
var TileCache = class {
  constructor(view) {
    this.view = view;
    this.buckets = buckets.map(() => []);
    this.index = buckets.map(() => 0);
    this.reused = /* @__PURE__ */ new Map();
  }
  // Put a tile in the cache.
  add(tile) {
    let i = tile.constructor.bucket, bucket = this.buckets[i];
    if (bucket.length < 6)
      bucket.push(tile);
    else
      bucket[
        this.index[i] = (this.index[i] + 1) % 6
        /* C.Bucket */
      ] = tile;
  }
  find(cls, test, type = 2) {
    let i = cls.bucket;
    let bucket = this.buckets[i], off = this.index[i];
    for (let j = bucket.length - 1; j >= 0; j--) {
      let index = (j + off) % bucket.length, tile = bucket[index];
      if ((!test || test(tile)) && !this.reused.has(tile)) {
        bucket.splice(index, 1);
        if (index < off)
          this.index[i]--;
        this.reused.set(tile, type);
        return tile;
      }
    }
    return null;
  }
  findWidget(widget, length, flags) {
    let widgets = this.buckets[0];
    if (widgets.length)
      for (let i = 0, pass = 0; ; i++) {
        if (i == widgets.length) {
          if (pass)
            return null;
          pass = 1;
          i = 0;
        }
        let tile = widgets[i];
        if (!this.reused.has(tile) && (pass == 0 ? tile.widget.compare(widget) : tile.widget.constructor == widget.constructor && widget.updateDOM(tile.dom, this.view))) {
          widgets.splice(i, 1);
          if (i < this.index[0])
            this.index[0]--;
          this.reused.set(
            tile,
            1
            /* Reused.Full */
          );
          tile.length = length;
          tile.flags = tile.flags & ~(496 | 1) | flags;
          return tile;
        }
      }
  }
  reuse(tile) {
    this.reused.set(
      tile,
      1
      /* Reused.Full */
    );
    return tile;
  }
  maybeReuse(tile, type = 2) {
    if (this.reused.has(tile))
      return void 0;
    this.reused.set(tile, type);
    return tile.dom;
  }
};
var TileUpdate = class {
  constructor(view, old, blockWrappers2, decorations2, disallowBlockEffectsFor) {
    this.view = view;
    this.decorations = decorations2;
    this.disallowBlockEffectsFor = disallowBlockEffectsFor;
    this.openWidget = false;
    this.openMarks = 0;
    this.cache = new TileCache(view);
    this.text = new TextStream(view.state.doc);
    this.builder = new TileBuilder(this.cache, new DocTile(view, view.contentDOM), RangeSet.iter(blockWrappers2));
    this.cache.reused.set(
      old,
      2
      /* Reused.DOM */
    );
    this.old = new TilePointer(old);
  }
  run(changes, composition) {
    let compositionContext = composition && this.getCompositionContext(composition.text);
    for (let posA = 0, posB = 0, i = 0; ; ) {
      let next = i < changes.length ? changes[i++] : null;
      let skipA = next ? next.fromA : this.old.root.length;
      if (skipA > posA) {
        let len = skipA - posA;
        this.preserve(len, !i, !next);
        posA = skipA;
        posB += len;
      }
      if (!next)
        break;
      this.forward(next.fromA, next.toA);
      if (composition && next.fromA <= composition.range.fromA && next.toA >= composition.range.toA) {
        this.emit(posB, composition.range.fromB);
        this.builder.addComposition(composition, compositionContext);
        this.emit(composition.range.toB, next.toB);
      } else {
        this.emit(posB, next.toB);
      }
      posB = next.toB;
      posA = next.toA;
    }
    if (this.builder.curLine)
      this.builder.endLine();
    return this.builder.root;
  }
  preserve(length, incStart, incEnd) {
    let activeMarks = getMarks(this.old), openMarks = this.openMarks;
    this.old.advance(length, incEnd ? 1 : -1, {
      skip: (tile, from, to) => {
        if (tile.isWidget()) {
          if (this.openWidget) {
            this.builder.continueWidget(to - from);
          } else {
            let widget = to > 0 || from < tile.length ? WidgetTile.of(tile.widget, this.view, to - from, tile.flags & 496, this.cache.maybeReuse(tile)) : this.cache.reuse(tile);
            if (widget.flags & 256) {
              widget.flags &= ~1;
              this.builder.addBlockWidget(widget);
            } else {
              this.builder.addInlineWidget(widget, activeMarks, openMarks);
              openMarks = activeMarks.length;
            }
          }
        } else if (tile.isText()) {
          if (!from && to == tile.length) {
            this.builder.addText(tile.text, activeMarks, openMarks, this.cache.reuse(tile));
          } else {
            this.cache.add(tile);
            this.builder.addText(tile.text.slice(from, to), activeMarks, openMarks);
          }
          openMarks = activeMarks.length;
        } else if (tile.isLine()) {
          tile.flags &= ~1;
          this.cache.reused.set(
            tile,
            1
            /* Reused.Full */
          );
          this.builder.addLine(tile);
        } else if (tile instanceof WidgetBufferTile) {
          this.cache.add(tile);
        } else if (tile instanceof MarkTile) {
          this.builder.addMark(tile, activeMarks, openMarks);
          this.cache.reused.set(
            tile,
            1
            /* Reused.Full */
          );
        } else {
          return false;
        }
        this.openWidget = false;
      },
      enter: (tile) => {
        if (tile.isLine()) {
          this.builder.addLineStart(tile.attrs, this.cache.maybeReuse(tile));
        } else {
          this.cache.add(tile);
          if (tile instanceof MarkTile) {
            activeMarks.unshift(tile.mark);
            openMarks++;
          }
        }
        this.openWidget = false;
      },
      leave: (tile) => {
        if (tile.isLine()) {
          if (activeMarks.length)
            activeMarks.length = openMarks = 0;
        } else if (tile instanceof MarkTile) {
          activeMarks.shift();
          openMarks = Math.min(openMarks, activeMarks.length);
        }
      },
      break: () => {
        this.builder.addBreak();
        this.openWidget = false;
      }
    });
    this.text.skip(length);
  }
  emit(from, to) {
    let pendingLineAttrs = null;
    let b = this.builder, markCount = 0;
    let openEnd = RangeSet.spans(this.decorations, from, to, {
      point: (from2, to2, deco, active, openStart, index) => {
        if (deco instanceof PointDecoration) {
          if (this.disallowBlockEffectsFor[index]) {
            if (deco.block)
              throw new RangeError("Block decorations may not be specified via plugins");
            if (to2 > this.view.state.doc.lineAt(from2).to)
              throw new RangeError("Decorations that replace line breaks may not be specified via plugins");
          }
          markCount = active.length;
          if (openStart > active.length) {
            b.continueWidget(to2 - from2);
          } else {
            let widget = deco.widget || (deco.block ? NullWidget.block : NullWidget.inline);
            let flags = widgetFlags(deco);
            let tile = this.cache.findWidget(widget, to2 - from2, flags) || WidgetTile.of(widget, this.view, to2 - from2, flags);
            if (deco.block) {
              if (deco.startSide > 0)
                b.addLineStartIfNotCovered(pendingLineAttrs);
              b.addBlockWidget(tile);
            } else {
              b.ensureLine(pendingLineAttrs);
              b.addInlineWidget(tile, active, openStart);
            }
          }
          pendingLineAttrs = null;
        } else {
          pendingLineAttrs = addLineDeco(pendingLineAttrs, deco);
        }
        if (to2 > from2)
          this.text.skip(to2 - from2);
      },
      span: (from2, to2, active, openStart) => {
        for (let pos = from2; pos < to2; ) {
          let chars = this.text.next(Math.min(512, to2 - pos));
          if (chars == null) {
            b.addLineStartIfNotCovered(pendingLineAttrs);
            b.addBreak();
            pos++;
          } else {
            b.ensureLine(pendingLineAttrs);
            b.addText(chars, active, openStart);
            pos += chars.length;
          }
          pendingLineAttrs = null;
        }
      }
    });
    b.addLineStartIfNotCovered(pendingLineAttrs);
    this.openWidget = openEnd > markCount;
    this.openMarks = openEnd;
  }
  forward(from, to) {
    this.old.advance(to - from, 1, {
      skip: (tile, from2, to2) => {
        if (tile.isText() || to2 == tile.length)
          this.cache.add(tile);
      },
      enter: (tile) => this.cache.add(tile),
      leave: () => {
      },
      break: () => {
      }
    });
  }
  getCompositionContext(text) {
    let marks2 = [], line = null;
    for (let parent = text.parentNode; ; parent = parent.parentNode) {
      let tile = Tile.get(parent);
      if (parent == this.view.contentDOM)
        break;
      if (tile instanceof MarkTile)
        marks2.push(tile);
      else if (tile === null || tile === void 0 ? void 0 : tile.isLine())
        line = tile;
      else if (parent.nodeName == "DIV" && !line && parent != this.view.contentDOM)
        line = new LineTile(parent, lineBaseAttrs);
      else
        marks2.push(MarkTile.of(new MarkDecoration({ tagName: parent.nodeName.toLowerCase(), attributes: getAttrs(parent) }), parent));
    }
    return { line, marks: marks2 };
  }
};
function hasContent(tile, requireText) {
  let scan = (tile2) => {
    for (let ch of tile2.children)
      if ((requireText ? ch.isText() : ch.length) || scan(ch))
        return true;
    return false;
  };
  return scan(tile);
}
function widgetFlags(deco) {
  let flags = deco.isReplace ? (deco.startSide < 0 ? 64 : 0) | (deco.endSide > 0 ? 128 : 0) : deco.startSide > 0 ? 32 : 16;
  if (deco.block)
    flags |= 256;
  return flags;
}
var lineBaseAttrs = { class: "cm-line" };
function addLineDeco(value, deco) {
  let attrs = deco.spec.attributes, cls = deco.spec.class;
  if (!attrs && !cls)
    return value;
  if (!value)
    value = { class: "cm-line" };
  if (attrs)
    combineAttrs(attrs, value);
  if (cls)
    value.class += " " + cls;
  return value;
}
function getMarks(ptr) {
  let found = [];
  for (let i = ptr.parents.length; i > 1; i--) {
    let tile = i == ptr.parents.length ? ptr.tile : ptr.parents[i].tile;
    if (tile instanceof MarkTile)
      found.push(tile.mark);
  }
  return found;
}
function freeNode(node) {
  let tile = Tile.get(node);
  if (tile)
    tile.setDOM(node.cloneNode());
  return node;
}
var NullWidget = class extends WidgetType {
  constructor(tag) {
    super();
    this.tag = tag;
  }
  eq(other) {
    return other.tag == this.tag;
  }
  toDOM() {
    return document.createElement(this.tag);
  }
  updateDOM(elt) {
    return elt.nodeName.toLowerCase() == this.tag;
  }
  get isHidden() {
    return true;
  }
};
NullWidget.inline = /* @__PURE__ */ new NullWidget("span");
NullWidget.block = /* @__PURE__ */ new NullWidget("div");
var BreakWidget = /* @__PURE__ */ new class extends WidgetType {
  toDOM() {
    return document.createElement("br");
  }
  get isHidden() {
    return true;
  }
  get editable() {
    return true;
  }
}();
var DocView = class {
  constructor(view) {
    this.view = view;
    this.decorations = [];
    this.blockWrappers = [];
    this.dynamicDecorationMap = [false];
    this.domChanged = null;
    this.hasComposition = null;
    this.editContextFormatting = Decoration.none;
    this.lastCompositionAfterCursor = false;
    this.minWidth = 0;
    this.minWidthFrom = 0;
    this.minWidthTo = 0;
    this.impreciseAnchor = null;
    this.impreciseHead = null;
    this.forceSelection = false;
    this.lastUpdate = Date.now();
    this.updateDeco();
    this.tile = new DocTile(view, view.contentDOM);
    this.updateInner([new ChangedRange(0, 0, 0, view.state.doc.length)], null);
  }
  // Update the document view to a given state.
  update(update) {
    var _a2;
    let changedRanges = update.changedRanges;
    if (this.minWidth > 0 && changedRanges.length) {
      if (!changedRanges.every(({ fromA, toA }) => toA < this.minWidthFrom || fromA > this.minWidthTo)) {
        this.minWidth = this.minWidthFrom = this.minWidthTo = 0;
      } else {
        this.minWidthFrom = update.changes.mapPos(this.minWidthFrom, 1);
        this.minWidthTo = update.changes.mapPos(this.minWidthTo, 1);
      }
    }
    this.updateEditContextFormatting(update);
    let readCompositionAt = -1;
    if (this.view.inputState.composing >= 0 && !this.view.observer.editContext) {
      if ((_a2 = this.domChanged) === null || _a2 === void 0 ? void 0 : _a2.newSel)
        readCompositionAt = this.domChanged.newSel.head;
      else if (!touchesComposition(update.changes, this.hasComposition) && !update.selectionSet)
        readCompositionAt = update.state.selection.main.head;
    }
    let composition = readCompositionAt > -1 ? findCompositionRange(this.view, update.changes, readCompositionAt) : null;
    this.domChanged = null;
    if (this.hasComposition) {
      let { from, to } = this.hasComposition;
      changedRanges = new ChangedRange(from, to, update.changes.mapPos(from, -1), update.changes.mapPos(to, 1)).addToSet(changedRanges.slice());
    }
    this.hasComposition = composition ? { from: composition.range.fromB, to: composition.range.toB } : null;
    if ((browser.ie || browser.chrome) && !composition && update && update.state.doc.lines != update.startState.doc.lines)
      this.forceSelection = true;
    let prevDeco = this.decorations, prevWrappers = this.blockWrappers;
    this.updateDeco();
    let decoDiff = findChangedDeco(prevDeco, this.decorations, update.changes);
    if (decoDiff.length)
      changedRanges = ChangedRange.extendWithRanges(changedRanges, decoDiff);
    let blockDiff = findChangedWrappers(prevWrappers, this.blockWrappers, update.changes);
    if (blockDiff.length)
      changedRanges = ChangedRange.extendWithRanges(changedRanges, blockDiff);
    if (composition && !changedRanges.some((r) => r.fromA <= composition.range.fromA && r.toA >= composition.range.toA))
      changedRanges = composition.range.addToSet(changedRanges.slice());
    if (this.tile.flags & 2 && changedRanges.length == 0) {
      return false;
    } else {
      this.updateInner(changedRanges, composition);
      if (update.transactions.length)
        this.lastUpdate = Date.now();
      return true;
    }
  }
  // Used by update and the constructor do perform the actual DOM
  // update
  updateInner(changes, composition) {
    this.view.viewState.mustMeasureContent = true;
    let { observer } = this.view;
    observer.ignore(() => {
      if (composition || changes.length) {
        let oldTile = this.tile;
        let builder = new TileUpdate(this.view, oldTile, this.blockWrappers, this.decorations, this.dynamicDecorationMap);
        this.tile = builder.run(changes, composition);
        destroyDropped(oldTile, builder.cache.reused);
      }
      this.tile.dom.style.height = this.view.viewState.contentHeight / this.view.scaleY + "px";
      this.tile.dom.style.flexBasis = this.minWidth ? this.minWidth + "px" : "";
      let track = browser.chrome || browser.ios ? { node: observer.selectionRange.focusNode, written: false } : void 0;
      this.tile.sync();
      if (track && (track.written || observer.selectionRange.focusNode != track.node))
        this.forceSelection = true;
      this.tile.dom.style.height = "";
    });
    let gaps = [];
    if (this.view.viewport.from || this.view.viewport.to < this.view.state.doc.length) {
      for (let child of this.tile.children)
        if (child.isWidget() && child.widget instanceof BlockGapWidget)
          gaps.push(child.dom);
    }
    observer.updateGaps(gaps);
  }
  updateEditContextFormatting(update) {
    this.editContextFormatting = this.editContextFormatting.map(update.changes);
    for (let tr of update.transactions)
      for (let effect of tr.effects)
        if (effect.is(setEditContextFormatting)) {
          this.editContextFormatting = effect.value;
        }
  }
  // Sync the DOM selection to this.state.selection
  updateSelection(mustRead = false, fromPointer = false) {
    if (mustRead || !this.view.observer.selectionRange.focusNode)
      this.view.observer.readSelectionRange();
    let { dom } = this.tile;
    let activeElt = this.view.root.activeElement, focused = activeElt == dom;
    let selectionNotFocus = !focused && !(this.view.state.facet(editable) || dom.tabIndex > -1) && hasSelection(dom, this.view.observer.selectionRange) && !(activeElt && dom.contains(activeElt));
    if (!(focused || fromPointer || selectionNotFocus))
      return;
    let force = this.forceSelection;
    this.forceSelection = false;
    let main = this.view.state.selection.main, anchor, head;
    if (main.empty) {
      head = anchor = this.inlineDOMNearPos(main.anchor, main.assoc || 1);
    } else {
      head = this.inlineDOMNearPos(main.head, main.head == main.from ? 1 : -1);
      anchor = this.inlineDOMNearPos(main.anchor, main.anchor == main.from ? 1 : -1);
    }
    if (browser.gecko && main.empty && !this.hasComposition && betweenUneditable(anchor)) {
      let dummy = document.createTextNode("");
      this.view.observer.ignore(() => anchor.node.insertBefore(dummy, anchor.node.childNodes[anchor.offset] || null));
      anchor = head = new DOMPos(dummy, 0);
      force = true;
    }
    let domSel = this.view.observer.selectionRange;
    if (force || !domSel.focusNode || (!isEquivalentPosition(anchor.node, anchor.offset, domSel.anchorNode, domSel.anchorOffset) || !isEquivalentPosition(head.node, head.offset, domSel.focusNode, domSel.focusOffset)) && !this.suppressWidgetCursorChange(domSel, main)) {
      this.view.observer.ignore(() => {
        if (browser.android && browser.chrome && dom.contains(domSel.focusNode) && inUneditable(domSel.focusNode, dom)) {
          dom.blur();
          dom.focus({ preventScroll: true });
        }
        let rawSel = getSelection(this.view.root);
        if (!rawSel) ;
        else if (main.empty) {
          if (browser.gecko) {
            let nextTo = nextToUneditable(anchor.node, anchor.offset);
            if (nextTo && nextTo != (1 | 2)) {
              let text = (nextTo == 1 ? textNodeBefore : textNodeAfter)(anchor.node, anchor.offset);
              if (text)
                anchor = new DOMPos(text.node, text.offset);
            }
          }
          rawSel.collapse(anchor.node, anchor.offset);
          if (main.bidiLevel != null && rawSel.caretBidiLevel !== void 0)
            rawSel.caretBidiLevel = main.bidiLevel;
        } else if (rawSel.extend) {
          rawSel.collapse(anchor.node, anchor.offset);
          try {
            rawSel.extend(head.node, head.offset);
          } catch (_) {
          }
        } else {
          let range = document.createRange();
          if (main.anchor > main.head)
            [anchor, head] = [head, anchor];
          range.setEnd(head.node, head.offset);
          range.setStart(anchor.node, anchor.offset);
          rawSel.removeAllRanges();
          rawSel.addRange(range);
        }
        if (selectionNotFocus && this.view.root.activeElement == dom) {
          dom.blur();
          if (activeElt)
            activeElt.focus();
        }
      });
      this.view.observer.setSelectionRange(anchor, head);
    }
    this.impreciseAnchor = anchor.precise ? null : new DOMPos(domSel.anchorNode, domSel.anchorOffset);
    this.impreciseHead = head.precise ? null : new DOMPos(domSel.focusNode, domSel.focusOffset);
  }
  // If a zero-length widget is inserted next to the cursor during
  // composition, avoid moving it across it and disrupting the
  // composition.
  suppressWidgetCursorChange(sel, cursor) {
    return this.hasComposition && cursor.empty && isEquivalentPosition(sel.focusNode, sel.focusOffset, sel.anchorNode, sel.anchorOffset) && this.posFromDOM(sel.focusNode, sel.focusOffset) == cursor.head;
  }
  enforceCursorAssoc() {
    if (this.hasComposition)
      return;
    let { view } = this, cursor = view.state.selection.main;
    let sel = getSelection(view.root);
    let { anchorNode, anchorOffset } = view.observer.selectionRange;
    if (!sel || !cursor.empty || !cursor.assoc || !sel.modify)
      return;
    let line = this.lineAt(cursor.head);
    if (!line)
      return;
    let lineStart = line.posAtStart;
    if (cursor.head == lineStart || cursor.head == lineStart + line.length)
      return;
    let before = this.coordsAt(cursor.head, -1), after = this.coordsAt(cursor.head, 1);
    if (!before || !after || before.bottom > after.top)
      return;
    let dom = this.domAtPos(cursor.head + cursor.assoc, cursor.assoc);
    sel.collapse(dom.node, dom.offset);
    sel.modify("move", cursor.assoc < 0 ? "forward" : "backward", "lineboundary");
    view.observer.readSelectionRange();
    let newRange = view.observer.selectionRange;
    if (view.docView.posFromDOM(newRange.anchorNode, newRange.anchorOffset) != cursor.from)
      sel.collapse(anchorNode, anchorOffset);
  }
  posFromDOM(node, offset) {
    let tile = this.tile.nearest(node);
    if (!tile)
      return this.tile.dom.compareDocumentPosition(node) & 2 ? 0 : this.view.state.doc.length;
    let start = tile.posAtStart;
    if (tile.isComposite()) {
      let after;
      if (node == tile.dom) {
        after = tile.dom.childNodes[offset];
      } else {
        let bias = maxOffset(node) == 0 ? 0 : offset == 0 ? -1 : 1;
        for (; ; ) {
          let parent = node.parentNode;
          if (parent == tile.dom)
            break;
          if (bias == 0 && parent.firstChild != parent.lastChild) {
            if (node == parent.firstChild)
              bias = -1;
            else
              bias = 1;
          }
          node = parent;
        }
        if (bias < 0)
          after = node;
        else
          after = node.nextSibling;
      }
      if (after == tile.dom.firstChild)
        return start;
      while (after && !Tile.get(after))
        after = after.nextSibling;
      if (!after)
        return start + tile.length;
      for (let i = 0, pos = start; ; i++) {
        let child = tile.children[i];
        if (child.dom == after)
          return pos;
        pos += child.length + child.breakAfter;
      }
    } else if (tile.isText()) {
      return node == tile.dom ? start + offset : start + (offset ? tile.length : 0);
    } else {
      return start;
    }
  }
  domAtPos(pos, side) {
    let { tile, offset } = this.tile.resolveBlock(pos, side);
    if (tile.isWidget())
      return tile.domPosFor(pos, side);
    return tile.domIn(offset, side);
  }
  inlineDOMNearPos(pos, side) {
    let before, beforeOff = -1, beforeBad = false;
    let after, afterOff = -1, afterBad = false;
    this.tile.blockTiles((tile, off) => {
      if (tile.isWidget()) {
        if (tile.flags & 32 && before)
          return true;
        if (tile.flags & 16)
          beforeBad = true;
      } else {
        let end = off + tile.length;
        if (off <= pos) {
          before = tile;
          beforeOff = pos - off;
          beforeBad = end < pos;
        }
        if (end >= pos && !after) {
          after = tile;
          afterOff = pos - off;
          afterBad = off > pos;
        }
        if (off > pos && after)
          return true;
      }
    });
    if (!before && !after)
      return this.domAtPos(pos, side);
    if (beforeBad && after)
      before = null;
    else if (afterBad && before)
      after = null;
    return before && side < 0 || !after ? before.domIn(beforeOff, side) : after.domIn(afterOff, side);
  }
  coordsAt(pos, side) {
    let { tile, offset } = this.tile.resolveBlock(pos, side);
    if (tile.isWidget()) {
      if (tile.widget instanceof BlockGapWidget)
        return null;
      return tile.coordsInWidget(offset, side, true);
    }
    return tile.coordsIn(offset, side);
  }
  lineAt(pos) {
    let { tile } = this.tile.resolveBlock(pos, 1);
    return tile.isLine() ? tile : null;
  }
  coordsForChar(pos) {
    let { tile, offset } = this.tile.resolveBlock(pos, 1);
    if (!tile.isLine())
      return null;
    function scan(tile2, offset2) {
      if (tile2.isComposite()) {
        for (let ch of tile2.children) {
          if (ch.length >= offset2) {
            let found = scan(ch, offset2);
            if (found)
              return found;
          }
          offset2 -= ch.length;
          if (offset2 < 0)
            break;
        }
      } else if (tile2.isText() && offset2 < tile2.length) {
        let end = findClusterBreak2(tile2.text, offset2);
        if (end == offset2)
          return null;
        let rects = textRange(tile2.dom, offset2, end).getClientRects();
        for (let i = 0; i < rects.length; i++) {
          let rect = rects[i];
          if (i == rects.length - 1 || rect.top < rect.bottom && rect.left < rect.right)
            return rect;
        }
      }
      return null;
    }
    return scan(tile, offset);
  }
  measureVisibleLineHeights(viewport) {
    let result = [], { from, to } = viewport;
    let contentWidth = this.view.contentDOM.clientWidth;
    let isWider = contentWidth > Math.max(this.view.scrollDOM.clientWidth, this.minWidth) + 1;
    let widest = -1, ltr = this.view.textDirection == Direction.LTR;
    let spaceAbove = 0;
    let scan = (tile, pos, measureBounds) => {
      for (let i = 0; i < tile.children.length; i++) {
        if (pos > to)
          break;
        let child = tile.children[i], end = pos + child.length;
        let childRect = child.dom.getBoundingClientRect(), { height } = childRect;
        if (measureBounds && !i)
          spaceAbove += childRect.top - measureBounds.top;
        if (child instanceof BlockWrapperTile) {
          if (end > from)
            scan(child, pos, childRect);
        } else if (pos >= from) {
          if (spaceAbove > 0)
            result.push(-spaceAbove);
          result.push(height + spaceAbove);
          spaceAbove = 0;
          if (isWider) {
            let last = child.dom.lastChild;
            let rects = last ? clientRectsFor(last) : [];
            if (rects.length) {
              let rect = rects[rects.length - 1];
              let width = ltr ? rect.right - childRect.left : childRect.right - rect.left;
              if (width > widest) {
                widest = width;
                this.minWidth = contentWidth;
                this.minWidthFrom = pos;
                this.minWidthTo = end;
              }
            }
          }
        }
        if (measureBounds && i == tile.children.length - 1)
          spaceAbove += measureBounds.bottom - childRect.bottom;
        pos = end + child.breakAfter;
      }
    };
    scan(this.tile, 0, null);
    return result;
  }
  textDirectionAt(pos) {
    let { tile } = this.tile.resolveBlock(pos, 1);
    return getComputedStyle(tile.dom).direction == "rtl" ? Direction.RTL : Direction.LTR;
  }
  measureTextSize() {
    let lineMeasure = this.tile.blockTiles((tile) => {
      if (tile.isLine() && tile.children.length && tile.length <= 20) {
        let totalWidth = 0, textHeight2;
        for (let child of tile.children) {
          if (!child.isText() || /[^ -~]/.test(child.text))
            return void 0;
          let rects = clientRectsFor(child.dom);
          if (rects.length != 1)
            return void 0;
          totalWidth += rects[0].width;
          textHeight2 = rects[0].height;
        }
        if (totalWidth)
          return {
            lineHeight: tile.dom.getBoundingClientRect().height,
            charWidth: totalWidth / tile.length,
            textHeight: textHeight2
          };
      }
    });
    if (lineMeasure)
      return lineMeasure;
    let dummy = document.createElement("div"), lineHeight, charWidth, textHeight;
    dummy.className = "cm-line";
    dummy.style.width = "99999px";
    dummy.style.position = "absolute";
    dummy.textContent = "abc def ghi jkl mno pqr stu";
    this.view.observer.ignore(() => {
      this.tile.dom.appendChild(dummy);
      let rect = clientRectsFor(dummy.firstChild)[0];
      lineHeight = dummy.getBoundingClientRect().height;
      charWidth = rect && rect.width ? rect.width / 27 : 7;
      textHeight = rect && rect.height ? rect.height : lineHeight;
      dummy.remove();
    });
    return { lineHeight, charWidth, textHeight };
  }
  computeBlockGapDeco() {
    let deco = [], vs = this.view.viewState;
    for (let pos = 0, i = 0; ; i++) {
      let next = i == vs.viewports.length ? null : vs.viewports[i];
      let end = next ? next.from - 1 : this.view.state.doc.length;
      if (end > pos) {
        let height = (vs.lineBlockAt(end).bottom - vs.lineBlockAt(pos).top) / this.view.scaleY;
        deco.push(Decoration.replace({
          widget: new BlockGapWidget(height),
          block: true,
          inclusive: true,
          isBlockGap: true
        }).range(pos, end));
      }
      if (!next)
        break;
      pos = next.to + 1;
    }
    return Decoration.set(deco);
  }
  updateDeco() {
    let i = 1;
    let allDeco = this.view.state.facet(decorations).map((d) => {
      let dynamic = this.dynamicDecorationMap[i++] = typeof d == "function";
      return dynamic ? d(this.view) : d;
    });
    let dynamicOuter = false, outerDeco = this.view.state.facet(outerDecorations).map((d, i2) => {
      let dynamic = typeof d == "function";
      if (dynamic)
        dynamicOuter = true;
      return dynamic ? d(this.view) : d;
    });
    if (outerDeco.length) {
      this.dynamicDecorationMap[i++] = dynamicOuter;
      allDeco.push(RangeSet.join(outerDeco));
    }
    this.decorations = [
      this.editContextFormatting,
      ...allDeco,
      this.computeBlockGapDeco(),
      this.view.viewState.lineGapDeco
    ];
    while (i < this.decorations.length)
      this.dynamicDecorationMap[i++] = false;
    this.blockWrappers = this.view.state.facet(blockWrappers).map((v) => typeof v == "function" ? v(this.view) : v);
  }
  scrollIntoView(target) {
    if (target.isSnapshot) {
      let ref = this.view.viewState.lineBlockAt(target.range.head);
      this.view.scrollDOM.scrollTop = ref.top - target.yMargin;
      this.view.scrollDOM.scrollLeft = target.xMargin;
      return;
    }
    for (let handler of this.view.state.facet(scrollHandler)) {
      try {
        if (handler(this.view, target.range, target))
          return true;
      } catch (e) {
        logException(this.view.state, e, "scroll handler");
      }
    }
    let { range } = target;
    let rect = this.coordsAt(range.head, range.empty ? range.assoc : range.head > range.anchor ? -1 : 1), other;
    if (!rect)
      return;
    if (!range.empty && (other = this.coordsAt(range.anchor, range.anchor > range.head ? -1 : 1)))
      rect = {
        left: Math.min(rect.left, other.left),
        top: Math.min(rect.top, other.top),
        right: Math.max(rect.right, other.right),
        bottom: Math.max(rect.bottom, other.bottom)
      };
    let margins = getScrollMargins(this.view);
    let targetRect = {
      left: rect.left - margins.left,
      top: rect.top - margins.top,
      right: rect.right + margins.right,
      bottom: rect.bottom + margins.bottom
    };
    let { offsetWidth, offsetHeight } = this.view.scrollDOM;
    scrollRectIntoView(this.view.scrollDOM, targetRect, range.head < range.anchor ? -1 : 1, target.x, target.y, Math.max(Math.min(target.xMargin, offsetWidth), -offsetWidth), Math.max(Math.min(target.yMargin, offsetHeight), -offsetHeight), this.view.textDirection == Direction.LTR);
  }
  lineHasWidget(pos) {
    let scan = (child) => child.isWidget() || child.children.some(scan);
    return scan(this.tile.resolveBlock(pos, 1).tile);
  }
  destroy() {
    destroyDropped(this.tile);
  }
};
function destroyDropped(tile, reused) {
  let r = reused === null || reused === void 0 ? void 0 : reused.get(tile);
  if (r != 1) {
    if (r == null)
      tile.destroy();
    for (let ch of tile.children)
      destroyDropped(ch, reused);
  }
}
function betweenUneditable(pos) {
  return pos.node.nodeType == 1 && pos.node.firstChild && (pos.offset == 0 || pos.node.childNodes[pos.offset - 1].contentEditable == "false") && (pos.offset == pos.node.childNodes.length || pos.node.childNodes[pos.offset].contentEditable == "false");
}
function findCompositionNode(view, headPos) {
  let sel = view.observer.selectionRange;
  if (!sel.focusNode)
    return null;
  let textBefore = textNodeBefore(sel.focusNode, sel.focusOffset);
  let textAfter = textNodeAfter(sel.focusNode, sel.focusOffset);
  let textNode = textBefore || textAfter;
  if (textAfter && textBefore && textAfter.node != textBefore.node) {
    let tileAfter = Tile.get(textAfter.node);
    if (!tileAfter || tileAfter.isText() && tileAfter.text != textAfter.node.nodeValue) {
      textNode = textAfter;
    } else if (view.docView.lastCompositionAfterCursor) {
      let tileBefore = Tile.get(textBefore.node);
      if (!(!tileBefore || tileBefore.isText() && tileBefore.text != textBefore.node.nodeValue))
        textNode = textAfter;
    }
  }
  view.docView.lastCompositionAfterCursor = textNode != textBefore;
  if (!textNode)
    return null;
  let from = headPos - textNode.offset;
  return { from, to: from + textNode.node.nodeValue.length, node: textNode.node };
}
function findCompositionRange(view, changes, headPos) {
  let found = findCompositionNode(view, headPos);
  if (!found)
    return null;
  let { node: textNode, from, to } = found, text = textNode.nodeValue;
  if (/[\n\r]/.test(text))
    return null;
  if (view.state.doc.sliceString(found.from, found.to) != text)
    return null;
  let inv = changes.invertedDesc;
  return { range: new ChangedRange(inv.mapPos(from), inv.mapPos(to), from, to), text: textNode };
}
function nextToUneditable(node, offset) {
  if (node.nodeType != 1)
    return 0;
  return (offset && node.childNodes[offset - 1].contentEditable == "false" ? 1 : 0) | (offset < node.childNodes.length && node.childNodes[offset].contentEditable == "false" ? 2 : 0);
}
var DecorationComparator$1 = class DecorationComparator {
  constructor() {
    this.changes = [];
  }
  compareRange(from, to) {
    addRange(from, to, this.changes);
  }
  comparePoint(from, to) {
    addRange(from, to, this.changes);
  }
  boundChange(pos) {
    addRange(pos, pos, this.changes);
  }
};
function findChangedDeco(a2, b, diff) {
  let comp = new DecorationComparator$1();
  RangeSet.compare(a2, b, diff, comp);
  return comp.changes;
}
var WrapperComparator = class {
  constructor() {
    this.changes = [];
  }
  compareRange(from, to) {
    addRange(from, to, this.changes);
  }
  comparePoint() {
  }
  boundChange(pos) {
    addRange(pos, pos, this.changes);
  }
};
function findChangedWrappers(a2, b, diff) {
  let comp = new WrapperComparator();
  RangeSet.compare(a2, b, diff, comp);
  return comp.changes;
}
function inUneditable(node, inside2) {
  for (let cur = node; cur && cur != inside2; cur = cur.assignedSlot || cur.parentNode) {
    if (cur.nodeType == 1 && cur.contentEditable == "false") {
      return true;
    }
  }
  return false;
}
function touchesComposition(changes, composition) {
  let touched = false;
  if (composition)
    changes.iterChangedRanges((from, to) => {
      if (from < composition.to && to > composition.from)
        touched = true;
    });
  return touched;
}
var BlockGapWidget = class extends WidgetType {
  constructor(height) {
    super();
    this.height = height;
  }
  toDOM() {
    let elt = document.createElement("div");
    elt.className = "cm-gap";
    this.updateDOM(elt);
    return elt;
  }
  eq(other) {
    return other.height == this.height;
  }
  updateDOM(elt) {
    elt.style.height = this.height + "px";
    return true;
  }
  get editable() {
    return true;
  }
  get estimatedHeight() {
    return this.height;
  }
  ignoreEvent() {
    return false;
  }
};
function groupAt(state, pos, bias = 1) {
  let categorize = state.charCategorizer(pos);
  let line = state.doc.lineAt(pos), linePos = pos - line.from;
  if (line.length == 0)
    return EditorSelection.cursor(pos);
  if (linePos == 0)
    bias = 1;
  else if (linePos == line.length)
    bias = -1;
  let from = linePos, to = linePos;
  if (bias < 0)
    from = findClusterBreak2(line.text, linePos, false);
  else
    to = findClusterBreak2(line.text, linePos);
  let cat = categorize(line.text.slice(from, to));
  while (from > 0) {
    let prev = findClusterBreak2(line.text, from, false);
    if (categorize(line.text.slice(prev, from)) != cat)
      break;
    from = prev;
  }
  while (to < line.length) {
    let next = findClusterBreak2(line.text, to);
    if (categorize(line.text.slice(to, next)) != cat)
      break;
    to = next;
  }
  return EditorSelection.range(from + line.from, to + line.from);
}
function posAtCoordsImprecise(view, contentRect, block, x, y) {
  let into = Math.round((x - contentRect.left) * view.defaultCharacterWidth);
  if (view.lineWrapping && block.height > view.defaultLineHeight * 1.5) {
    let textHeight = view.viewState.heightOracle.textHeight;
    let line = Math.floor((y - block.top - (view.defaultLineHeight - textHeight) * 0.5) / textHeight);
    into += line * view.viewState.heightOracle.lineLength;
  }
  let content2 = view.state.sliceDoc(block.from, block.to);
  return block.from + findColumn(content2, into, view.state.tabSize);
}
function blockAt(view, pos, side) {
  let line = view.lineBlockAt(pos);
  if (Array.isArray(line.type)) {
    let best;
    for (let l of line.type) {
      if (l.from > pos)
        break;
      if (l.to < pos)
        continue;
      if (l.from < pos && l.to > pos)
        return l;
      if (!best || l.type == BlockType.Text && (best.type != l.type || (side < 0 ? l.from < pos : l.to > pos)))
        best = l;
    }
    return best || line;
  }
  return line;
}
function moveToLineBoundary(view, start, forward, includeWrap) {
  let line = blockAt(view, start.head, start.assoc || -1);
  let coords = !includeWrap || line.type != BlockType.Text || !(view.lineWrapping || line.widgetLineBreaks) ? null : view.coordsAtPos(start.assoc < 0 && start.head > line.from ? start.head - 1 : start.head);
  if (coords) {
    let editorRect = view.dom.getBoundingClientRect();
    let direction = view.textDirectionAt(line.from);
    let pos = view.posAtCoords({
      x: forward == (direction == Direction.LTR) ? editorRect.right - 1 : editorRect.left + 1,
      y: (coords.top + coords.bottom) / 2
    });
    if (pos != null)
      return EditorSelection.cursor(pos, forward ? -1 : 1);
  }
  return EditorSelection.cursor(forward ? line.to : line.from, forward ? -1 : 1);
}
function moveByChar(view, start, forward, by) {
  let line = view.state.doc.lineAt(start.head), spans = view.bidiSpans(line);
  let direction = view.textDirectionAt(line.from);
  for (let cur = start, check = null; ; ) {
    let next = moveVisually(line, spans, direction, cur, forward), char = movedOver;
    if (!next) {
      if (line.number == (forward ? view.state.doc.lines : 1))
        return cur;
      char = "\n";
      line = view.state.doc.line(line.number + (forward ? 1 : -1));
      spans = view.bidiSpans(line);
      next = view.visualLineSide(line, !forward);
    }
    if (!check) {
      if (!by)
        return next;
      check = by(char);
    } else if (!check(char)) {
      return cur;
    }
    cur = next;
  }
}
function byGroup(view, pos, start) {
  let categorize = view.state.charCategorizer(pos);
  let cat = categorize(start);
  return (next) => {
    let nextCat = categorize(next);
    if (cat == CharCategory.Space)
      cat = nextCat;
    return cat == nextCat;
  };
}
function moveVertically(view, start, forward, distance) {
  let startPos = start.head, dir = forward ? 1 : -1;
  if (startPos == (forward ? view.state.doc.length : 0))
    return EditorSelection.cursor(startPos, start.assoc);
  let goal = start.goalColumn, startY;
  let rect = view.contentDOM.getBoundingClientRect();
  let startCoords = view.coordsAtPos(startPos, start.assoc || -1), docTop = view.documentTop;
  if (startCoords) {
    if (goal == null)
      goal = startCoords.left - rect.left;
    startY = dir < 0 ? startCoords.top : startCoords.bottom;
  } else {
    let line = view.viewState.lineBlockAt(startPos);
    if (goal == null)
      goal = Math.min(rect.right - rect.left, view.defaultCharacterWidth * (startPos - line.from));
    startY = (dir < 0 ? line.top : line.bottom) + docTop;
  }
  let resolvedGoal = rect.left + goal;
  let dist2 = distance !== null && distance !== void 0 ? distance : view.viewState.heightOracle.textHeight >> 1;
  for (let extra = 0; ; extra += 10) {
    let curY = startY + (dist2 + extra) * dir;
    let pos = posAtCoords(view, { x: resolvedGoal, y: curY }, false, dir);
    return EditorSelection.cursor(pos.pos, pos.assoc, void 0, goal);
  }
}
function skipAtomicRanges(atoms, pos, bias) {
  for (; ; ) {
    let moved = 0;
    for (let set of atoms) {
      set.between(pos - 1, pos + 1, (from, to, value) => {
        if (pos > from && pos < to) {
          let side = moved || bias || (pos - from < to - pos ? -1 : 1);
          pos = side < 0 ? from : to;
          moved = side;
        }
      });
    }
    if (!moved)
      return pos;
  }
}
function skipAtomsForSelection(atoms, sel) {
  let ranges = null;
  for (let i = 0; i < sel.ranges.length; i++) {
    let range = sel.ranges[i], updated = null;
    if (range.empty) {
      let pos = skipAtomicRanges(atoms, range.from, 0);
      if (pos != range.from)
        updated = EditorSelection.cursor(pos, -1);
    } else {
      let from = skipAtomicRanges(atoms, range.from, -1);
      let to = skipAtomicRanges(atoms, range.to, 1);
      if (from != range.from || to != range.to)
        updated = EditorSelection.range(range.from == range.anchor ? from : to, range.from == range.head ? from : to);
    }
    if (updated) {
      if (!ranges)
        ranges = sel.ranges.slice();
      ranges[i] = updated;
    }
  }
  return ranges ? EditorSelection.create(ranges, sel.mainIndex) : sel;
}
function skipAtoms(view, oldPos, pos) {
  let newPos = skipAtomicRanges(view.state.facet(atomicRanges).map((f) => f(view)), pos.from, oldPos.head > pos.from ? -1 : 1);
  return newPos == pos.from ? pos : EditorSelection.cursor(newPos, newPos < pos.from ? 1 : -1);
}
var PosAssoc = class {
  constructor(pos, assoc) {
    this.pos = pos;
    this.assoc = assoc;
  }
};
function posAtCoords(view, coords, precise, scanY) {
  let content2 = view.contentDOM.getBoundingClientRect(), docTop = content2.top + view.viewState.paddingTop;
  let { x, y } = coords, yOffset = y - docTop, block;
  for (; ; ) {
    if (yOffset < 0)
      return new PosAssoc(0, 1);
    if (yOffset > view.viewState.docHeight)
      return new PosAssoc(view.state.doc.length, -1);
    block = view.elementAtHeight(yOffset);
    if (scanY == null || block.type == BlockType.Text)
      break;
    let halfLine = view.viewState.heightOracle.textHeight / 2;
    yOffset = scanY > 0 ? block.bottom + halfLine : block.top - halfLine;
  }
  if (view.viewport.from >= block.to || view.viewport.to <= block.from) {
    if (precise)
      return null;
    if (block.type == BlockType.Text) {
      let pos = posAtCoordsImprecise(view, content2, block, x, y);
      return new PosAssoc(pos, pos == block.from ? 1 : -1);
    }
  }
  if (block.type != BlockType.Text)
    return yOffset < (block.top + block.bottom) / 2 ? new PosAssoc(block.from, 1) : new PosAssoc(block.to, -1);
  return posAtCoordsInline(view, view.docView.lineAt(block.from), block.from, x, y);
}
function posAtCoordsInline(view, tile, offset, x, y) {
  let closest = -1, closestRect = null;
  let dxClosest = 1e9, dyClosest = 1e9;
  let rowTop = y, rowBot = y;
  let checkRects = (rects, index) => {
    for (let i = 0; i < rects.length; i++) {
      let rect = rects[i];
      if (rect.top == rect.bottom)
        continue;
      let dx = rect.left > x ? rect.left - x : rect.right < x ? x - rect.right : 0;
      let dy = rect.top > y ? rect.top - y : rect.bottom < y ? y - rect.bottom : 0;
      if (rect.top <= rowBot && rect.bottom >= rowTop) {
        rowTop = Math.min(rect.top, rowTop);
        rowBot = Math.max(rect.bottom, rowBot);
        dy = 0;
      }
      if (closest < 0 || (dy - dyClosest || dx - dxClosest) < 0) {
        if (closest >= 0 && dyClosest && dxClosest < dx && closestRect.top <= rowBot - 2 && closestRect.bottom >= rowTop + 2) {
          dyClosest = 0;
        } else {
          closest = index;
          dxClosest = dx;
          dyClosest = dy;
          closestRect = rect;
        }
      }
    }
  };
  if (tile.isText()) {
    for (let i = 0; i < tile.length; ) {
      let next = findClusterBreak2(tile.text, i);
      checkRects(textRange(tile.dom, i, next).getClientRects(), i);
      if (!dxClosest && !dyClosest)
        break;
      i = next;
    }
    let after = x > (closestRect.left + closestRect.right) / 2 == (dirAt(view, closest + offset) == Direction.LTR);
    return after ? new PosAssoc(offset + findClusterBreak2(tile.text, closest), -1) : new PosAssoc(offset + closest, 1);
  } else {
    if (!tile.length)
      return new PosAssoc(offset, 1);
    for (let i = 0; i < tile.children.length; i++) {
      let child = tile.children[i];
      if (child.flags & 48)
        continue;
      let rects = (child.dom.nodeType == 1 ? child.dom : textRange(child.dom, 0, child.length)).getClientRects();
      checkRects(rects, i);
      if (!dxClosest && !dyClosest)
        break;
    }
    let inner = tile.children[closest], innerOff = tile.posBefore(inner, offset);
    if (inner.isComposite() || inner.isText())
      return posAtCoordsInline(view, inner, innerOff, Math.max(closestRect.left, Math.min(closestRect.right, x)), y);
    let after = x > (closestRect.left + closestRect.right) / 2 == (dirAt(view, closest + offset) == Direction.LTR);
    return after ? new PosAssoc(innerOff + inner.length, -1) : new PosAssoc(innerOff, 1);
  }
}
function dirAt(view, pos) {
  let line = view.state.doc.lineAt(pos), spans = view.bidiSpans(line);
  return spans[BidiSpan.find(view.bidiSpans(line), pos - line.from, -1, 1)].dir;
}
var LineBreakPlaceholder = "\uFFFF";
var DOMReader = class {
  constructor(points, state) {
    this.points = points;
    this.text = "";
    this.lineSeparator = state.facet(EditorState.lineSeparator);
  }
  append(text) {
    this.text += text;
  }
  lineBreak() {
    this.text += LineBreakPlaceholder;
  }
  readRange(start, end) {
    if (!start)
      return this;
    let parent = start.parentNode;
    for (let cur = start; ; ) {
      this.findPointBefore(parent, cur);
      let oldLen = this.text.length;
      this.readNode(cur);
      let tile = Tile.get(cur), next = cur.nextSibling;
      if (next == end) {
        if ((tile === null || tile === void 0 ? void 0 : tile.breakAfter) && !next)
          this.lineBreak();
        break;
      }
      let nextTile = Tile.get(next);
      if ((tile && nextTile ? tile.breakAfter : (tile ? tile.breakAfter : isBlockElement(cur)) || isBlockElement(next) && (cur.nodeName != "BR" || (tile === null || tile === void 0 ? void 0 : tile.isWidget())) && this.text.length > oldLen) && !isEmptyToEnd(next, end))
        this.lineBreak();
      cur = next;
    }
    this.findPointBefore(parent, end);
    return this;
  }
  readTextNode(node) {
    let text = node.nodeValue;
    for (let point of this.points)
      if (point.node == node)
        point.pos = this.text.length + Math.min(point.offset, text.length);
    for (let off = 0, re = this.lineSeparator ? null : /\r\n?|\n/g; ; ) {
      let nextBreak = -1, breakSize = 1, m;
      if (this.lineSeparator) {
        nextBreak = text.indexOf(this.lineSeparator, off);
        breakSize = this.lineSeparator.length;
      } else if (m = re.exec(text)) {
        nextBreak = m.index;
        breakSize = m[0].length;
      }
      this.append(text.slice(off, nextBreak < 0 ? text.length : nextBreak));
      if (nextBreak < 0)
        break;
      this.lineBreak();
      if (breakSize > 1) {
        for (let point of this.points)
          if (point.node == node && point.pos > this.text.length)
            point.pos -= breakSize - 1;
      }
      off = nextBreak + breakSize;
    }
  }
  readNode(node) {
    let tile = Tile.get(node);
    let fromView = tile && tile.overrideDOMText;
    if (fromView != null) {
      this.findPointInside(node, fromView.length);
      for (let i = fromView.iter(); !i.next().done; ) {
        if (i.lineBreak)
          this.lineBreak();
        else
          this.append(i.value);
      }
    } else if (node.nodeType == 3) {
      this.readTextNode(node);
    } else if (node.nodeName == "BR") {
      if (node.nextSibling)
        this.lineBreak();
    } else if (node.nodeType == 1) {
      this.readRange(node.firstChild, null);
    }
  }
  findPointBefore(node, next) {
    for (let point of this.points)
      if (point.node == node && node.childNodes[point.offset] == next)
        point.pos = this.text.length;
  }
  findPointInside(node, length) {
    for (let point of this.points)
      if (node.nodeType == 3 ? point.node == node : node.contains(point.node))
        point.pos = this.text.length + (isAtEnd(node, point.node, point.offset) ? length : 0);
  }
};
function isAtEnd(parent, node, offset) {
  for (; ; ) {
    if (!node || offset < maxOffset(node))
      return false;
    if (node == parent)
      return true;
    offset = domIndex(node) + 1;
    node = node.parentNode;
  }
}
function isEmptyToEnd(node, end) {
  let widgets;
  for (; ; node = node.nextSibling) {
    if (node == end || !node)
      break;
    let view = Tile.get(node);
    if (!(view === null || view === void 0 ? void 0 : view.isWidget()))
      return false;
    if (view)
      (widgets || (widgets = [])).push(view);
  }
  if (widgets)
    for (let w of widgets) {
      let override = w.overrideDOMText;
      if (override === null || override === void 0 ? void 0 : override.length)
        return false;
    }
  return true;
}
var DOMPoint = class {
  constructor(node, offset) {
    this.node = node;
    this.offset = offset;
    this.pos = -1;
  }
};
var DOMChange = class {
  constructor(view, start, end, typeOver) {
    this.typeOver = typeOver;
    this.bounds = null;
    this.text = "";
    this.domChanged = start > -1;
    let { impreciseHead: iHead, impreciseAnchor: iAnchor } = view.docView;
    if (view.state.readOnly && start > -1) {
      this.newSel = null;
    } else if (start > -1 && (this.bounds = domBoundsAround(view.docView.tile, start, end, 0))) {
      let selPoints = iHead || iAnchor ? [] : selectionPoints(view);
      let reader = new DOMReader(selPoints, view.state);
      reader.readRange(this.bounds.startDOM, this.bounds.endDOM);
      this.text = reader.text;
      this.newSel = selectionFromPoints(selPoints, this.bounds.from);
    } else {
      let domSel = view.observer.selectionRange;
      let head = iHead && iHead.node == domSel.focusNode && iHead.offset == domSel.focusOffset || !contains(view.contentDOM, domSel.focusNode) ? view.state.selection.main.head : view.docView.posFromDOM(domSel.focusNode, domSel.focusOffset);
      let anchor = iAnchor && iAnchor.node == domSel.anchorNode && iAnchor.offset == domSel.anchorOffset || !contains(view.contentDOM, domSel.anchorNode) ? view.state.selection.main.anchor : view.docView.posFromDOM(domSel.anchorNode, domSel.anchorOffset);
      let vp = view.viewport;
      if ((browser.ios || browser.chrome) && view.state.selection.main.empty && head != anchor && (vp.from > 0 || vp.to < view.state.doc.length)) {
        let from = Math.min(head, anchor), to = Math.max(head, anchor);
        let offFrom = vp.from - from, offTo = vp.to - to;
        if ((offFrom == 0 || offFrom == 1 || from == 0) && (offTo == 0 || offTo == -1 || to == view.state.doc.length)) {
          head = 0;
          anchor = view.state.doc.length;
        }
      }
      if (view.inputState.composing > -1 && view.state.selection.ranges.length > 1)
        this.newSel = view.state.selection.replaceRange(EditorSelection.range(anchor, head));
      else
        this.newSel = EditorSelection.single(anchor, head);
    }
  }
};
function domBoundsAround(tile, from, to, offset) {
  if (tile.isComposite()) {
    let fromI = -1, fromStart = -1, toI = -1, toEnd = -1;
    for (let i = 0, pos = offset, prevEnd = offset; i < tile.children.length; i++) {
      let child = tile.children[i], end = pos + child.length;
      if (pos < from && end > to)
        return domBoundsAround(child, from, to, pos);
      if (end >= from && fromI == -1) {
        fromI = i;
        fromStart = pos;
      }
      if (pos > to && child.dom.parentNode == tile.dom) {
        toI = i;
        toEnd = prevEnd;
        break;
      }
      prevEnd = end;
      pos = end + child.breakAfter;
    }
    return {
      from: fromStart,
      to: toEnd < 0 ? offset + tile.length : toEnd,
      startDOM: (fromI ? tile.children[fromI - 1].dom.nextSibling : null) || tile.dom.firstChild,
      endDOM: toI < tile.children.length && toI >= 0 ? tile.children[toI].dom : null
    };
  } else if (tile.isText()) {
    return { from: offset, to: offset + tile.length, startDOM: tile.dom, endDOM: tile.dom.nextSibling };
  } else {
    return null;
  }
}
function applyDOMChange(view, domChange) {
  let change;
  let { newSel } = domChange, sel = view.state.selection.main;
  let lastKey = view.inputState.lastKeyTime > Date.now() - 100 ? view.inputState.lastKeyCode : -1;
  if (domChange.bounds) {
    let { from, to } = domChange.bounds;
    let preferredPos = sel.from, preferredSide = null;
    if (lastKey === 8 || browser.android && domChange.text.length < to - from) {
      preferredPos = sel.to;
      preferredSide = "end";
    }
    let diff = findDiff(view.state.doc.sliceString(from, to, LineBreakPlaceholder), domChange.text, preferredPos - from, preferredSide);
    if (diff) {
      if (browser.chrome && lastKey == 13 && diff.toB == diff.from + 2 && domChange.text.slice(diff.from, diff.toB) == LineBreakPlaceholder + LineBreakPlaceholder)
        diff.toB--;
      change = {
        from: from + diff.from,
        to: from + diff.toA,
        insert: Text.of(domChange.text.slice(diff.from, diff.toB).split(LineBreakPlaceholder))
      };
    }
  } else if (newSel && (!view.hasFocus && view.state.facet(editable) || newSel.main.eq(sel))) {
    newSel = null;
  }
  if (!change && !newSel)
    return false;
  if (!change && domChange.typeOver && !sel.empty && newSel && newSel.main.empty) {
    change = { from: sel.from, to: sel.to, insert: view.state.doc.slice(sel.from, sel.to) };
  } else if ((browser.mac || browser.android) && change && change.from == change.to && change.from == sel.head - 1 && /^\. ?$/.test(change.insert.toString()) && view.contentDOM.getAttribute("autocorrect") == "off") {
    if (newSel && change.insert.length == 2)
      newSel = EditorSelection.single(newSel.main.anchor - 1, newSel.main.head - 1);
    change = { from: change.from, to: change.to, insert: Text.of([change.insert.toString().replace(".", " ")]) };
  } else if (change && change.from >= sel.from && change.to <= sel.to && (change.from != sel.from || change.to != sel.to) && sel.to - sel.from - (change.to - change.from) <= 4) {
    change = {
      from: sel.from,
      to: sel.to,
      insert: view.state.doc.slice(sel.from, change.from).append(change.insert).append(view.state.doc.slice(change.to, sel.to))
    };
  } else if (view.state.doc.lineAt(sel.from).to < sel.to && view.docView.lineHasWidget(sel.to) && view.inputState.insertingTextAt > Date.now() - 50) {
    change = {
      from: sel.from,
      to: sel.to,
      insert: view.state.toText(view.inputState.insertingText)
    };
  } else if (browser.chrome && change && change.from == change.to && change.from == sel.head && change.insert.toString() == "\n " && view.lineWrapping) {
    if (newSel)
      newSel = EditorSelection.single(newSel.main.anchor - 1, newSel.main.head - 1);
    change = { from: sel.from, to: sel.to, insert: Text.of([" "]) };
  }
  if (change) {
    return applyDOMChangeInner(view, change, newSel, lastKey);
  } else if (newSel && !newSel.main.eq(sel)) {
    let scrollIntoView2 = false, userEvent = "select";
    if (view.inputState.lastSelectionTime > Date.now() - 50) {
      if (view.inputState.lastSelectionOrigin == "select")
        scrollIntoView2 = true;
      userEvent = view.inputState.lastSelectionOrigin;
      if (userEvent == "select.pointer")
        newSel = skipAtomsForSelection(view.state.facet(atomicRanges).map((f) => f(view)), newSel);
    }
    view.dispatch({ selection: newSel, scrollIntoView: scrollIntoView2, userEvent });
    return true;
  } else {
    return false;
  }
}
function applyDOMChangeInner(view, change, newSel, lastKey = -1) {
  if (browser.ios && view.inputState.flushIOSKey(change))
    return true;
  let sel = view.state.selection.main;
  if (browser.android && (change.to == sel.to && // GBoard will sometimes remove a space it just inserted
  // after a completion when you press enter
  (change.from == sel.from || change.from == sel.from - 1 && view.state.sliceDoc(change.from, sel.from) == " ") && change.insert.length == 1 && change.insert.lines == 2 && dispatchKey(view.contentDOM, "Enter", 13) || (change.from == sel.from - 1 && change.to == sel.to && change.insert.length == 0 || lastKey == 8 && change.insert.length < change.to - change.from && change.to > sel.head) && dispatchKey(view.contentDOM, "Backspace", 8) || change.from == sel.from && change.to == sel.to + 1 && change.insert.length == 0 && dispatchKey(view.contentDOM, "Delete", 46)))
    return true;
  let text = change.insert.toString();
  if (view.inputState.composing >= 0)
    view.inputState.composing++;
  let defaultTr;
  let defaultInsert = () => defaultTr || (defaultTr = applyDefaultInsert(view, change, newSel));
  if (!view.state.facet(inputHandler).some((h) => h(view, change.from, change.to, text, defaultInsert)))
    view.dispatch(defaultInsert());
  return true;
}
function applyDefaultInsert(view, change, newSel) {
  let tr, startState = view.state, sel = startState.selection.main, inAtomic = -1;
  if (change.from == change.to && change.from < sel.from || change.from > sel.to) {
    let side = change.from < sel.from ? -1 : 1, pos = side < 0 ? sel.from : sel.to;
    let moved = skipAtomicRanges(startState.facet(atomicRanges).map((f) => f(view)), pos, side);
    if (change.from == moved)
      inAtomic = moved;
  }
  if (inAtomic > -1) {
    tr = {
      changes: change,
      selection: EditorSelection.cursor(change.from + change.insert.length, -1)
    };
  } else if (change.from >= sel.from && change.to <= sel.to && change.to - change.from >= (sel.to - sel.from) / 3 && (!newSel || newSel.main.empty && newSel.main.from == change.from + change.insert.length) && view.inputState.composing < 0) {
    let before = sel.from < change.from ? startState.sliceDoc(sel.from, change.from) : "";
    let after = sel.to > change.to ? startState.sliceDoc(change.to, sel.to) : "";
    tr = startState.replaceSelection(view.state.toText(before + change.insert.sliceString(0, void 0, view.state.lineBreak) + after));
  } else {
    let changes = startState.changes(change);
    let mainSel = newSel && newSel.main.to <= changes.newLength ? newSel.main : void 0;
    if (startState.selection.ranges.length > 1 && (view.inputState.composing >= 0 || view.inputState.compositionPendingChange) && change.to <= sel.to + 10 && change.to >= sel.to - 10) {
      let replaced = view.state.sliceDoc(change.from, change.to);
      let compositionRange, composition = newSel && findCompositionNode(view, newSel.main.head);
      if (composition) {
        let dLen = change.insert.length - (change.to - change.from);
        compositionRange = { from: composition.from, to: composition.to - dLen };
      } else {
        compositionRange = view.state.doc.lineAt(sel.head);
      }
      let offset = sel.to - change.to;
      tr = startState.changeByRange((range) => {
        if (range.from == sel.from && range.to == sel.to)
          return { changes, range: mainSel || range.map(changes) };
        let to = range.to - offset, from = to - replaced.length;
        if (view.state.sliceDoc(from, to) != replaced || // Unfortunately, there's no way to make multiple
        // changes in the same node work without aborting
        // composition, so cursors in the composition range are
        // ignored.
        to >= compositionRange.from && from <= compositionRange.to)
          return { range };
        let rangeChanges = startState.changes({ from, to, insert: change.insert }), selOff = range.to - sel.to;
        return {
          changes: rangeChanges,
          range: !mainSel ? range.map(rangeChanges) : EditorSelection.range(Math.max(0, mainSel.anchor + selOff), Math.max(0, mainSel.head + selOff))
        };
      });
    } else {
      tr = {
        changes,
        selection: mainSel && startState.selection.replaceRange(mainSel)
      };
    }
  }
  let userEvent = "input.type";
  if (view.composing || view.inputState.compositionPendingChange && view.inputState.compositionEndedAt > Date.now() - 50) {
    view.inputState.compositionPendingChange = false;
    userEvent += ".compose";
    if (view.inputState.compositionFirstChange) {
      userEvent += ".start";
      view.inputState.compositionFirstChange = false;
    }
  }
  return startState.update(tr, { userEvent, scrollIntoView: true });
}
function findDiff(a2, b, preferredPos, preferredSide) {
  let minLen = Math.min(a2.length, b.length);
  let from = 0;
  while (from < minLen && a2.charCodeAt(from) == b.charCodeAt(from))
    from++;
  if (from == minLen && a2.length == b.length)
    return null;
  let toA = a2.length, toB = b.length;
  while (toA > 0 && toB > 0 && a2.charCodeAt(toA - 1) == b.charCodeAt(toB - 1)) {
    toA--;
    toB--;
  }
  if (preferredSide == "end") {
    let adjust = Math.max(0, from - Math.min(toA, toB));
    preferredPos -= toA + adjust - from;
  }
  if (toA < from && a2.length < b.length) {
    let move = preferredPos <= from && preferredPos >= toA ? from - preferredPos : 0;
    from -= move;
    toB = from + (toB - toA);
    toA = from;
  } else if (toB < from) {
    let move = preferredPos <= from && preferredPos >= toB ? from - preferredPos : 0;
    from -= move;
    toA = from + (toA - toB);
    toB = from;
  }
  return { from, toA, toB };
}
function selectionPoints(view) {
  let result = [];
  if (view.root.activeElement != view.contentDOM)
    return result;
  let { anchorNode, anchorOffset, focusNode, focusOffset } = view.observer.selectionRange;
  if (anchorNode) {
    result.push(new DOMPoint(anchorNode, anchorOffset));
    if (focusNode != anchorNode || focusOffset != anchorOffset)
      result.push(new DOMPoint(focusNode, focusOffset));
  }
  return result;
}
function selectionFromPoints(points, base2) {
  if (points.length == 0)
    return null;
  let anchor = points[0].pos, head = points.length == 2 ? points[1].pos : anchor;
  return anchor > -1 && head > -1 ? EditorSelection.single(anchor + base2, head + base2) : null;
}
var InputState = class {
  setSelectionOrigin(origin) {
    this.lastSelectionOrigin = origin;
    this.lastSelectionTime = Date.now();
  }
  constructor(view) {
    this.view = view;
    this.lastKeyCode = 0;
    this.lastKeyTime = 0;
    this.lastTouchTime = 0;
    this.lastFocusTime = 0;
    this.lastScrollTop = 0;
    this.lastScrollLeft = 0;
    this.pendingIOSKey = void 0;
    this.tabFocusMode = -1;
    this.lastSelectionOrigin = null;
    this.lastSelectionTime = 0;
    this.lastContextMenu = 0;
    this.scrollHandlers = [];
    this.handlers = /* @__PURE__ */ Object.create(null);
    this.composing = -1;
    this.compositionFirstChange = null;
    this.compositionEndedAt = 0;
    this.compositionPendingKey = false;
    this.compositionPendingChange = false;
    this.insertingText = "";
    this.insertingTextAt = 0;
    this.mouseSelection = null;
    this.draggedContent = null;
    this.handleEvent = this.handleEvent.bind(this);
    this.notifiedFocused = view.hasFocus;
    if (browser.safari)
      view.contentDOM.addEventListener("input", () => null);
    if (browser.gecko)
      firefoxCopyCutHack(view.contentDOM.ownerDocument);
  }
  handleEvent(event) {
    if (!eventBelongsToEditor(this.view, event) || this.ignoreDuringComposition(event))
      return;
    if (event.type == "keydown" && this.keydown(event))
      return;
    if (this.view.updateState != 0)
      Promise.resolve().then(() => this.runHandlers(event.type, event));
    else
      this.runHandlers(event.type, event);
  }
  runHandlers(type, event) {
    let handlers2 = this.handlers[type];
    if (handlers2) {
      for (let observer of handlers2.observers)
        observer(this.view, event);
      for (let handler of handlers2.handlers) {
        if (event.defaultPrevented)
          break;
        if (handler(this.view, event)) {
          event.preventDefault();
          break;
        }
      }
    }
  }
  ensureHandlers(plugins) {
    let handlers2 = computeHandlers(plugins), prev = this.handlers, dom = this.view.contentDOM;
    for (let type in handlers2)
      if (type != "scroll") {
        let passive = !handlers2[type].handlers.length;
        let exists = prev[type];
        if (exists && passive != !exists.handlers.length) {
          dom.removeEventListener(type, this.handleEvent);
          exists = null;
        }
        if (!exists)
          dom.addEventListener(type, this.handleEvent, { passive });
      }
    for (let type in prev)
      if (type != "scroll" && !handlers2[type])
        dom.removeEventListener(type, this.handleEvent);
    this.handlers = handlers2;
  }
  keydown(event) {
    this.lastKeyCode = event.keyCode;
    this.lastKeyTime = Date.now();
    if (event.keyCode == 9 && this.tabFocusMode > -1 && (!this.tabFocusMode || Date.now() <= this.tabFocusMode))
      return true;
    if (this.tabFocusMode > 0 && event.keyCode != 27 && modifierCodes.indexOf(event.keyCode) < 0)
      this.tabFocusMode = -1;
    if (browser.android && browser.chrome && !event.synthetic && (event.keyCode == 13 || event.keyCode == 8)) {
      this.view.observer.delayAndroidKey(event.key, event.keyCode);
      return true;
    }
    let pending;
    if (browser.ios && !event.synthetic && !event.altKey && !event.metaKey && ((pending = PendingKeys.find((key) => key.keyCode == event.keyCode)) && !event.ctrlKey || EmacsyPendingKeys.indexOf(event.key) > -1 && event.ctrlKey && !event.shiftKey)) {
      this.pendingIOSKey = pending || event;
      setTimeout(() => this.flushIOSKey(), 250);
      return true;
    }
    if (event.keyCode != 229)
      this.view.observer.forceFlush();
    return false;
  }
  flushIOSKey(change) {
    let key = this.pendingIOSKey;
    if (!key)
      return false;
    if (key.key == "Enter" && change && change.from < change.to && /^\S+$/.test(change.insert.toString()))
      return false;
    this.pendingIOSKey = void 0;
    return dispatchKey(this.view.contentDOM, key.key, key.keyCode, key instanceof KeyboardEvent ? key : void 0);
  }
  ignoreDuringComposition(event) {
    if (!/^key/.test(event.type) || event.synthetic)
      return false;
    if (this.composing > 0)
      return true;
    if (browser.safari && !browser.ios && this.compositionPendingKey && Date.now() - this.compositionEndedAt < 100) {
      this.compositionPendingKey = false;
      return true;
    }
    return false;
  }
  startMouseSelection(mouseSelection) {
    if (this.mouseSelection)
      this.mouseSelection.destroy();
    this.mouseSelection = mouseSelection;
  }
  update(update) {
    this.view.observer.update(update);
    if (this.mouseSelection)
      this.mouseSelection.update(update);
    if (this.draggedContent && update.docChanged)
      this.draggedContent = this.draggedContent.map(update.changes);
    if (update.transactions.length)
      this.lastKeyCode = this.lastSelectionTime = 0;
  }
  destroy() {
    if (this.mouseSelection)
      this.mouseSelection.destroy();
  }
};
function bindHandler(plugin, handler) {
  return (view, event) => {
    try {
      return handler.call(plugin, event, view);
    } catch (e) {
      logException(view.state, e);
    }
  };
}
function computeHandlers(plugins) {
  let result = /* @__PURE__ */ Object.create(null);
  function record(type) {
    return result[type] || (result[type] = { observers: [], handlers: [] });
  }
  for (let plugin of plugins) {
    let spec = plugin.spec, handlers2 = spec && spec.plugin.domEventHandlers, observers2 = spec && spec.plugin.domEventObservers;
    if (handlers2)
      for (let type in handlers2) {
        let f = handlers2[type];
        if (f)
          record(type).handlers.push(bindHandler(plugin.value, f));
      }
    if (observers2)
      for (let type in observers2) {
        let f = observers2[type];
        if (f)
          record(type).observers.push(bindHandler(plugin.value, f));
      }
  }
  for (let type in handlers)
    record(type).handlers.push(handlers[type]);
  for (let type in observers)
    record(type).observers.push(observers[type]);
  return result;
}
var PendingKeys = [
  { key: "Backspace", keyCode: 8, inputType: "deleteContentBackward" },
  { key: "Enter", keyCode: 13, inputType: "insertParagraph" },
  { key: "Enter", keyCode: 13, inputType: "insertLineBreak" },
  { key: "Delete", keyCode: 46, inputType: "deleteContentForward" }
];
var EmacsyPendingKeys = "dthko";
var modifierCodes = [16, 17, 18, 20, 91, 92, 224, 225];
var dragScrollMargin = 6;
function dragScrollSpeed(dist2) {
  return Math.max(0, dist2) * 0.7 + 8;
}
function dist(a2, b) {
  return Math.max(Math.abs(a2.clientX - b.clientX), Math.abs(a2.clientY - b.clientY));
}
var MouseSelection = class {
  constructor(view, startEvent, style, mustSelect) {
    this.view = view;
    this.startEvent = startEvent;
    this.style = style;
    this.mustSelect = mustSelect;
    this.scrollSpeed = { x: 0, y: 0 };
    this.scrolling = -1;
    this.lastEvent = startEvent;
    this.scrollParents = scrollableParents(view.contentDOM);
    this.atoms = view.state.facet(atomicRanges).map((f) => f(view));
    let doc2 = view.contentDOM.ownerDocument;
    doc2.addEventListener("mousemove", this.move = this.move.bind(this));
    doc2.addEventListener("mouseup", this.up = this.up.bind(this));
    this.extend = startEvent.shiftKey;
    this.multiple = view.state.facet(EditorState.allowMultipleSelections) && addsSelectionRange(view, startEvent);
    this.dragging = isInPrimarySelection(view, startEvent) && getClickType(startEvent) == 1 ? null : false;
  }
  start(event) {
    if (this.dragging === false)
      this.select(event);
  }
  move(event) {
    if (event.buttons == 0)
      return this.destroy();
    if (this.dragging || this.dragging == null && dist(this.startEvent, event) < 10)
      return;
    this.select(this.lastEvent = event);
    let sx = 0, sy = 0;
    let left = 0, top2 = 0, right = this.view.win.innerWidth, bottom = this.view.win.innerHeight;
    if (this.scrollParents.x)
      ({ left, right } = this.scrollParents.x.getBoundingClientRect());
    if (this.scrollParents.y)
      ({ top: top2, bottom } = this.scrollParents.y.getBoundingClientRect());
    let margins = getScrollMargins(this.view);
    if (event.clientX - margins.left <= left + dragScrollMargin)
      sx = -dragScrollSpeed(left - event.clientX);
    else if (event.clientX + margins.right >= right - dragScrollMargin)
      sx = dragScrollSpeed(event.clientX - right);
    if (event.clientY - margins.top <= top2 + dragScrollMargin)
      sy = -dragScrollSpeed(top2 - event.clientY);
    else if (event.clientY + margins.bottom >= bottom - dragScrollMargin)
      sy = dragScrollSpeed(event.clientY - bottom);
    this.setScrollSpeed(sx, sy);
  }
  up(event) {
    if (this.dragging == null)
      this.select(this.lastEvent);
    if (!this.dragging)
      event.preventDefault();
    this.destroy();
  }
  destroy() {
    this.setScrollSpeed(0, 0);
    let doc2 = this.view.contentDOM.ownerDocument;
    doc2.removeEventListener("mousemove", this.move);
    doc2.removeEventListener("mouseup", this.up);
    this.view.inputState.mouseSelection = this.view.inputState.draggedContent = null;
  }
  setScrollSpeed(sx, sy) {
    this.scrollSpeed = { x: sx, y: sy };
    if (sx || sy) {
      if (this.scrolling < 0)
        this.scrolling = setInterval(() => this.scroll(), 50);
    } else if (this.scrolling > -1) {
      clearInterval(this.scrolling);
      this.scrolling = -1;
    }
  }
  scroll() {
    let { x, y } = this.scrollSpeed;
    if (x && this.scrollParents.x) {
      this.scrollParents.x.scrollLeft += x;
      x = 0;
    }
    if (y && this.scrollParents.y) {
      this.scrollParents.y.scrollTop += y;
      y = 0;
    }
    if (x || y)
      this.view.win.scrollBy(x, y);
    if (this.dragging === false)
      this.select(this.lastEvent);
  }
  select(event) {
    let { view } = this, selection = skipAtomsForSelection(this.atoms, this.style.get(event, this.extend, this.multiple));
    if (this.mustSelect || !selection.eq(view.state.selection, this.dragging === false))
      this.view.dispatch({
        selection,
        userEvent: "select.pointer"
      });
    this.mustSelect = false;
  }
  update(update) {
    if (update.transactions.some((tr) => tr.isUserEvent("input.type")))
      this.destroy();
    else if (this.style.update(update))
      setTimeout(() => this.select(this.lastEvent), 20);
  }
};
function addsSelectionRange(view, event) {
  let facet = view.state.facet(clickAddsSelectionRange);
  return facet.length ? facet[0](event) : browser.mac ? event.metaKey : event.ctrlKey;
}
function dragMovesSelection(view, event) {
  let facet = view.state.facet(dragMovesSelection$1);
  return facet.length ? facet[0](event) : browser.mac ? !event.altKey : !event.ctrlKey;
}
function isInPrimarySelection(view, event) {
  let { main } = view.state.selection;
  if (main.empty)
    return false;
  let sel = getSelection(view.root);
  if (!sel || sel.rangeCount == 0)
    return true;
  let rects = sel.getRangeAt(0).getClientRects();
  for (let i = 0; i < rects.length; i++) {
    let rect = rects[i];
    if (rect.left <= event.clientX && rect.right >= event.clientX && rect.top <= event.clientY && rect.bottom >= event.clientY)
      return true;
  }
  return false;
}
function eventBelongsToEditor(view, event) {
  if (!event.bubbles)
    return true;
  if (event.defaultPrevented)
    return false;
  for (let node = event.target, tile; node != view.contentDOM; node = node.parentNode)
    if (!node || node.nodeType == 11 || (tile = Tile.get(node)) && tile.isWidget() && tile.widget.ignoreEvent(event))
      return false;
  return true;
}
var handlers = /* @__PURE__ */ Object.create(null);
var observers = /* @__PURE__ */ Object.create(null);
var brokenClipboardAPI = browser.ie && browser.ie_version < 15 || browser.ios && browser.webkit_version < 604;
function capturePaste(view) {
  let parent = view.dom.parentNode;
  if (!parent)
    return;
  let target = parent.appendChild(document.createElement("textarea"));
  target.style.cssText = "position: fixed; left: -10000px; top: 10px";
  target.focus();
  setTimeout(() => {
    view.focus();
    target.remove();
    doPaste(view, target.value);
  }, 50);
}
function textFilter(state, facet, text) {
  for (let filter of state.facet(facet))
    text = filter(text, state);
  return text;
}
function doPaste(view, input) {
  input = textFilter(view.state, clipboardInputFilter, input);
  let { state } = view, changes, i = 1, text = state.toText(input);
  let byLine = text.lines == state.selection.ranges.length;
  let linewise = lastLinewiseCopy != null && state.selection.ranges.every((r) => r.empty) && lastLinewiseCopy == text.toString();
  if (linewise) {
    let lastLine = -1;
    changes = state.changeByRange((range) => {
      let line = state.doc.lineAt(range.from);
      if (line.from == lastLine)
        return { range };
      lastLine = line.from;
      let insert2 = state.toText((byLine ? text.line(i++).text : input) + state.lineBreak);
      return {
        changes: { from: line.from, insert: insert2 },
        range: EditorSelection.cursor(range.from + insert2.length)
      };
    });
  } else if (byLine) {
    changes = state.changeByRange((range) => {
      let line = text.line(i++);
      return {
        changes: { from: range.from, to: range.to, insert: line.text },
        range: EditorSelection.cursor(range.from + line.length)
      };
    });
  } else {
    changes = state.replaceSelection(text);
  }
  view.dispatch(changes, {
    userEvent: "input.paste",
    scrollIntoView: true
  });
}
observers.scroll = (view) => {
  view.inputState.lastScrollTop = view.scrollDOM.scrollTop;
  view.inputState.lastScrollLeft = view.scrollDOM.scrollLeft;
};
handlers.keydown = (view, event) => {
  view.inputState.setSelectionOrigin("select");
  if (event.keyCode == 27 && view.inputState.tabFocusMode != 0)
    view.inputState.tabFocusMode = Date.now() + 2e3;
  return false;
};
observers.touchstart = (view, e) => {
  view.inputState.lastTouchTime = Date.now();
  view.inputState.setSelectionOrigin("select.pointer");
};
observers.touchmove = (view) => {
  view.inputState.setSelectionOrigin("select.pointer");
};
handlers.mousedown = (view, event) => {
  view.observer.flush();
  if (view.inputState.lastTouchTime > Date.now() - 2e3)
    return false;
  let style = null;
  for (let makeStyle of view.state.facet(mouseSelectionStyle)) {
    style = makeStyle(view, event);
    if (style)
      break;
  }
  if (!style && event.button == 0)
    style = basicMouseSelection(view, event);
  if (style) {
    let mustFocus = !view.hasFocus;
    view.inputState.startMouseSelection(new MouseSelection(view, event, style, mustFocus));
    if (mustFocus)
      view.observer.ignore(() => {
        focusPreventScroll(view.contentDOM);
        let active = view.root.activeElement;
        if (active && !active.contains(view.contentDOM))
          active.blur();
      });
    let mouseSel = view.inputState.mouseSelection;
    if (mouseSel) {
      mouseSel.start(event);
      return mouseSel.dragging === false;
    }
  } else {
    view.inputState.setSelectionOrigin("select.pointer");
  }
  return false;
};
function rangeForClick(view, pos, bias, type) {
  if (type == 1) {
    return EditorSelection.cursor(pos, bias);
  } else if (type == 2) {
    return groupAt(view.state, pos, bias);
  } else {
    let visual = view.docView.lineAt(pos), line = view.state.doc.lineAt(visual ? visual.posAtEnd : pos);
    let from = visual ? visual.posAtStart : line.from, to = visual ? visual.posAtEnd : line.to;
    if (to < view.state.doc.length && to == line.to)
      to++;
    return EditorSelection.range(from, to);
  }
}
var inside = (x, y, rect) => y >= rect.top && y <= rect.bottom && x >= rect.left && x <= rect.right;
function findPositionSide(view, pos, x, y) {
  let line = view.docView.lineAt(pos);
  if (!line)
    return 1;
  let off = pos - line.posAtStart;
  if (off == 0)
    return 1;
  if (off == line.length)
    return -1;
  let before = line.coordsIn(off, -1);
  if (before && inside(x, y, before))
    return -1;
  let after = line.coordsIn(off, 1);
  if (after && inside(x, y, after))
    return 1;
  return before && before.bottom >= y ? -1 : 1;
}
function queryPos(view, event) {
  let pos = view.posAtCoords({ x: event.clientX, y: event.clientY }, false);
  return { pos, bias: findPositionSide(view, pos, event.clientX, event.clientY) };
}
var BadMouseDetail = browser.ie && browser.ie_version <= 11;
var lastMouseDown = null;
var lastMouseDownCount = 0;
var lastMouseDownTime = 0;
function getClickType(event) {
  if (!BadMouseDetail)
    return event.detail;
  let last = lastMouseDown, lastTime = lastMouseDownTime;
  lastMouseDown = event;
  lastMouseDownTime = Date.now();
  return lastMouseDownCount = !last || lastTime > Date.now() - 400 && Math.abs(last.clientX - event.clientX) < 2 && Math.abs(last.clientY - event.clientY) < 2 ? (lastMouseDownCount + 1) % 3 : 1;
}
function basicMouseSelection(view, event) {
  let start = queryPos(view, event), type = getClickType(event);
  let startSel = view.state.selection;
  return {
    update(update) {
      if (update.docChanged) {
        start.pos = update.changes.mapPos(start.pos);
        startSel = startSel.map(update.changes);
      }
    },
    get(event2, extend, multiple) {
      let cur = queryPos(view, event2), removed;
      let range = rangeForClick(view, cur.pos, cur.bias, type);
      if (start.pos != cur.pos && !extend) {
        let startRange = rangeForClick(view, start.pos, start.bias, type);
        let from = Math.min(startRange.from, range.from), to = Math.max(startRange.to, range.to);
        range = from < range.from ? EditorSelection.range(from, to) : EditorSelection.range(to, from);
      }
      if (extend)
        return startSel.replaceRange(startSel.main.extend(range.from, range.to));
      else if (multiple && type == 1 && startSel.ranges.length > 1 && (removed = removeRangeAround(startSel, cur.pos)))
        return removed;
      else if (multiple)
        return startSel.addRange(range);
      else
        return EditorSelection.create([range]);
    }
  };
}
function removeRangeAround(sel, pos) {
  for (let i = 0; i < sel.ranges.length; i++) {
    let { from, to } = sel.ranges[i];
    if (from <= pos && to >= pos)
      return EditorSelection.create(sel.ranges.slice(0, i).concat(sel.ranges.slice(i + 1)), sel.mainIndex == i ? 0 : sel.mainIndex - (sel.mainIndex > i ? 1 : 0));
  }
  return null;
}
handlers.dragstart = (view, event) => {
  let { selection: { main: range } } = view.state;
  if (event.target.draggable) {
    let tile = view.docView.tile.nearest(event.target);
    if (tile && tile.isWidget()) {
      let from = tile.posAtStart, to = from + tile.length;
      if (from >= range.to || to <= range.from)
        range = EditorSelection.range(from, to);
    }
  }
  let { inputState } = view;
  if (inputState.mouseSelection)
    inputState.mouseSelection.dragging = true;
  inputState.draggedContent = range;
  if (event.dataTransfer) {
    event.dataTransfer.setData("Text", textFilter(view.state, clipboardOutputFilter, view.state.sliceDoc(range.from, range.to)));
    event.dataTransfer.effectAllowed = "copyMove";
  }
  return false;
};
handlers.dragend = (view) => {
  view.inputState.draggedContent = null;
  return false;
};
function dropText(view, event, text, direct) {
  text = textFilter(view.state, clipboardInputFilter, text);
  if (!text)
    return;
  let dropPos = view.posAtCoords({ x: event.clientX, y: event.clientY }, false);
  let { draggedContent } = view.inputState;
  let del = direct && draggedContent && dragMovesSelection(view, event) ? { from: draggedContent.from, to: draggedContent.to } : null;
  let ins = { from: dropPos, insert: text };
  let changes = view.state.changes(del ? [del, ins] : ins);
  view.focus();
  view.dispatch({
    changes,
    selection: { anchor: changes.mapPos(dropPos, -1), head: changes.mapPos(dropPos, 1) },
    userEvent: del ? "move.drop" : "input.drop"
  });
  view.inputState.draggedContent = null;
}
handlers.drop = (view, event) => {
  if (!event.dataTransfer)
    return false;
  if (view.state.readOnly)
    return true;
  let files = event.dataTransfer.files;
  if (files && files.length) {
    let text = Array(files.length), read = 0;
    let finishFile = () => {
      if (++read == files.length)
        dropText(view, event, text.filter((s) => s != null).join(view.state.lineBreak), false);
    };
    for (let i = 0; i < files.length; i++) {
      let reader = new FileReader();
      reader.onerror = finishFile;
      reader.onload = () => {
        if (!/[\x00-\x08\x0e-\x1f]{2}/.test(reader.result))
          text[i] = reader.result;
        finishFile();
      };
      reader.readAsText(files[i]);
    }
    return true;
  } else {
    let text = event.dataTransfer.getData("Text");
    if (text) {
      dropText(view, event, text, true);
      return true;
    }
  }
  return false;
};
handlers.paste = (view, event) => {
  if (view.state.readOnly)
    return true;
  view.observer.flush();
  let data = brokenClipboardAPI ? null : event.clipboardData;
  if (data) {
    doPaste(view, data.getData("text/plain") || data.getData("text/uri-list"));
    return true;
  } else {
    capturePaste(view);
    return false;
  }
};
function captureCopy(view, text) {
  let parent = view.dom.parentNode;
  if (!parent)
    return;
  let target = parent.appendChild(document.createElement("textarea"));
  target.style.cssText = "position: fixed; left: -10000px; top: 10px";
  target.value = text;
  target.focus();
  target.selectionEnd = text.length;
  target.selectionStart = 0;
  setTimeout(() => {
    target.remove();
    view.focus();
  }, 50);
}
function copiedRange(state) {
  let content2 = [], ranges = [], linewise = false;
  for (let range of state.selection.ranges)
    if (!range.empty) {
      content2.push(state.sliceDoc(range.from, range.to));
      ranges.push(range);
    }
  if (!content2.length) {
    let upto = -1;
    for (let { from } of state.selection.ranges) {
      let line = state.doc.lineAt(from);
      if (line.number > upto) {
        content2.push(line.text);
        ranges.push({ from: line.from, to: Math.min(state.doc.length, line.to + 1) });
      }
      upto = line.number;
    }
    linewise = true;
  }
  return { text: textFilter(state, clipboardOutputFilter, content2.join(state.lineBreak)), ranges, linewise };
}
var lastLinewiseCopy = null;
handlers.copy = handlers.cut = (view, event) => {
  let { text, ranges, linewise } = copiedRange(view.state);
  if (!text && !linewise)
    return false;
  lastLinewiseCopy = linewise ? text : null;
  if (event.type == "cut" && !view.state.readOnly)
    view.dispatch({
      changes: ranges,
      scrollIntoView: true,
      userEvent: "delete.cut"
    });
  let data = brokenClipboardAPI ? null : event.clipboardData;
  if (data) {
    data.clearData();
    data.setData("text/plain", text);
    return true;
  } else {
    captureCopy(view, text);
    return false;
  }
};
var isFocusChange = /* @__PURE__ */ Annotation.define();
function focusChangeTransaction(state, focus) {
  let effects = [];
  for (let getEffect of state.facet(focusChangeEffect)) {
    let effect = getEffect(state, focus);
    if (effect)
      effects.push(effect);
  }
  return effects.length ? state.update({ effects, annotations: isFocusChange.of(true) }) : null;
}
function updateForFocusChange(view) {
  setTimeout(() => {
    let focus = view.hasFocus;
    if (focus != view.inputState.notifiedFocused) {
      let tr = focusChangeTransaction(view.state, focus);
      if (tr)
        view.dispatch(tr);
      else
        view.update([]);
    }
  }, 10);
}
observers.focus = (view) => {
  view.inputState.lastFocusTime = Date.now();
  if (!view.scrollDOM.scrollTop && (view.inputState.lastScrollTop || view.inputState.lastScrollLeft)) {
    view.scrollDOM.scrollTop = view.inputState.lastScrollTop;
    view.scrollDOM.scrollLeft = view.inputState.lastScrollLeft;
  }
  updateForFocusChange(view);
};
observers.blur = (view) => {
  view.observer.clearSelectionRange();
  updateForFocusChange(view);
};
observers.compositionstart = observers.compositionupdate = (view) => {
  if (view.observer.editContext)
    return;
  if (view.inputState.compositionFirstChange == null)
    view.inputState.compositionFirstChange = true;
  if (view.inputState.composing < 0) {
    view.inputState.composing = 0;
  }
};
observers.compositionend = (view) => {
  if (view.observer.editContext)
    return;
  view.inputState.composing = -1;
  view.inputState.compositionEndedAt = Date.now();
  view.inputState.compositionPendingKey = true;
  view.inputState.compositionPendingChange = view.observer.pendingRecords().length > 0;
  view.inputState.compositionFirstChange = null;
  if (browser.chrome && browser.android) {
    view.observer.flushSoon();
  } else if (view.inputState.compositionPendingChange) {
    Promise.resolve().then(() => view.observer.flush());
  } else {
    setTimeout(() => {
      if (view.inputState.composing < 0 && view.docView.hasComposition)
        view.update([]);
    }, 50);
  }
};
observers.contextmenu = (view) => {
  view.inputState.lastContextMenu = Date.now();
};
handlers.beforeinput = (view, event) => {
  var _a2, _b;
  if (event.inputType == "insertText" || event.inputType == "insertCompositionText") {
    view.inputState.insertingText = event.data;
    view.inputState.insertingTextAt = Date.now();
  }
  if (event.inputType == "insertReplacementText" && view.observer.editContext) {
    let text = (_a2 = event.dataTransfer) === null || _a2 === void 0 ? void 0 : _a2.getData("text/plain"), ranges = event.getTargetRanges();
    if (text && ranges.length) {
      let r = ranges[0];
      let from = view.posAtDOM(r.startContainer, r.startOffset), to = view.posAtDOM(r.endContainer, r.endOffset);
      applyDOMChangeInner(view, { from, to, insert: view.state.toText(text) }, null);
      return true;
    }
  }
  let pending;
  if (browser.chrome && browser.android && (pending = PendingKeys.find((key) => key.inputType == event.inputType))) {
    view.observer.delayAndroidKey(pending.key, pending.keyCode);
    if (pending.key == "Backspace" || pending.key == "Delete") {
      let startViewHeight = ((_b = window.visualViewport) === null || _b === void 0 ? void 0 : _b.height) || 0;
      setTimeout(() => {
        var _a3;
        if ((((_a3 = window.visualViewport) === null || _a3 === void 0 ? void 0 : _a3.height) || 0) > startViewHeight + 10 && view.hasFocus) {
          view.contentDOM.blur();
          view.focus();
        }
      }, 100);
    }
  }
  if (browser.ios && event.inputType == "deleteContentForward") {
    view.observer.flushSoon();
  }
  if (browser.safari && event.inputType == "insertText" && view.inputState.composing >= 0) {
    setTimeout(() => observers.compositionend(view, event), 20);
  }
  return false;
};
var appliedFirefoxHack = /* @__PURE__ */ new Set();
function firefoxCopyCutHack(doc2) {
  if (!appliedFirefoxHack.has(doc2)) {
    appliedFirefoxHack.add(doc2);
    doc2.addEventListener("copy", () => {
    });
    doc2.addEventListener("cut", () => {
    });
  }
}
var wrappingWhiteSpace = ["pre-wrap", "normal", "pre-line", "break-spaces"];
var heightChangeFlag = false;
function clearHeightChangeFlag() {
  heightChangeFlag = false;
}
var HeightOracle = class {
  constructor(lineWrapping) {
    this.lineWrapping = lineWrapping;
    this.doc = Text.empty;
    this.heightSamples = {};
    this.lineHeight = 14;
    this.charWidth = 7;
    this.textHeight = 14;
    this.lineLength = 30;
  }
  heightForGap(from, to) {
    let lines = this.doc.lineAt(to).number - this.doc.lineAt(from).number + 1;
    if (this.lineWrapping)
      lines += Math.max(0, Math.ceil((to - from - lines * this.lineLength * 0.5) / this.lineLength));
    return this.lineHeight * lines;
  }
  heightForLine(length) {
    if (!this.lineWrapping)
      return this.lineHeight;
    let lines = 1 + Math.max(0, Math.ceil((length - this.lineLength) / Math.max(1, this.lineLength - 5)));
    return lines * this.lineHeight;
  }
  setDoc(doc2) {
    this.doc = doc2;
    return this;
  }
  mustRefreshForWrapping(whiteSpace) {
    return wrappingWhiteSpace.indexOf(whiteSpace) > -1 != this.lineWrapping;
  }
  mustRefreshForHeights(lineHeights) {
    let newHeight = false;
    for (let i = 0; i < lineHeights.length; i++) {
      let h = lineHeights[i];
      if (h < 0) {
        i++;
      } else if (!this.heightSamples[Math.floor(h * 10)]) {
        newHeight = true;
        this.heightSamples[Math.floor(h * 10)] = true;
      }
    }
    return newHeight;
  }
  refresh(whiteSpace, lineHeight, charWidth, textHeight, lineLength, knownHeights) {
    let lineWrapping = wrappingWhiteSpace.indexOf(whiteSpace) > -1;
    let changed = Math.round(lineHeight) != Math.round(this.lineHeight) || this.lineWrapping != lineWrapping;
    this.lineWrapping = lineWrapping;
    this.lineHeight = lineHeight;
    this.charWidth = charWidth;
    this.textHeight = textHeight;
    this.lineLength = lineLength;
    if (changed) {
      this.heightSamples = {};
      for (let i = 0; i < knownHeights.length; i++) {
        let h = knownHeights[i];
        if (h < 0)
          i++;
        else
          this.heightSamples[Math.floor(h * 10)] = true;
      }
    }
    return changed;
  }
};
var MeasuredHeights = class {
  constructor(from, heights) {
    this.from = from;
    this.heights = heights;
    this.index = 0;
  }
  get more() {
    return this.index < this.heights.length;
  }
};
var BlockInfo = class _BlockInfo {
  /**
  @internal
  */
  constructor(from, length, top2, height, _content) {
    this.from = from;
    this.length = length;
    this.top = top2;
    this.height = height;
    this._content = _content;
  }
  /**
  The type of element this is. When querying lines, this may be
  an array of all the blocks that make up the line.
  */
  get type() {
    return typeof this._content == "number" ? BlockType.Text : Array.isArray(this._content) ? this._content : this._content.type;
  }
  /**
  The end of the element as a document position.
  */
  get to() {
    return this.from + this.length;
  }
  /**
  The bottom position of the element.
  */
  get bottom() {
    return this.top + this.height;
  }
  /**
  If this is a widget block, this will return the widget
  associated with it.
  */
  get widget() {
    return this._content instanceof PointDecoration ? this._content.widget : null;
  }
  /**
  If this is a textblock, this holds the number of line breaks
  that appear in widgets inside the block.
  */
  get widgetLineBreaks() {
    return typeof this._content == "number" ? this._content : 0;
  }
  /**
  @internal
  */
  join(other) {
    let content2 = (Array.isArray(this._content) ? this._content : [this]).concat(Array.isArray(other._content) ? other._content : [other]);
    return new _BlockInfo(this.from, this.length + other.length, this.top, this.height + other.height, content2);
  }
};
var QueryType = /* @__PURE__ */ (function(QueryType2) {
  QueryType2[QueryType2["ByPos"] = 0] = "ByPos";
  QueryType2[QueryType2["ByHeight"] = 1] = "ByHeight";
  QueryType2[QueryType2["ByPosNoHeight"] = 2] = "ByPosNoHeight";
  return QueryType2;
})(QueryType || (QueryType = {}));
var Epsilon = 1e-3;
var HeightMap = class _HeightMap {
  constructor(length, height, flags = 2) {
    this.length = length;
    this.height = height;
    this.flags = flags;
  }
  get outdated() {
    return (this.flags & 2) > 0;
  }
  set outdated(value) {
    this.flags = (value ? 2 : 0) | this.flags & ~2;
  }
  setHeight(height) {
    if (this.height != height) {
      if (Math.abs(this.height - height) > Epsilon)
        heightChangeFlag = true;
      this.height = height;
    }
  }
  // Base case is to replace a leaf node, which simply builds a tree
  // from the new nodes and returns that (HeightMapBranch and
  // HeightMapGap override this to actually use from/to)
  replace(_from, _to, nodes) {
    return _HeightMap.of(nodes);
  }
  // Again, these are base cases, and are overridden for branch and gap nodes.
  decomposeLeft(_to, result) {
    result.push(this);
  }
  decomposeRight(_from, result) {
    result.push(this);
  }
  applyChanges(decorations2, oldDoc, oracle, changes) {
    let me = this, doc2 = oracle.doc;
    for (let i = changes.length - 1; i >= 0; i--) {
      let { fromA, toA, fromB, toB } = changes[i];
      let start = me.lineAt(fromA, QueryType.ByPosNoHeight, oracle.setDoc(oldDoc), 0, 0);
      let end = start.to >= toA ? start : me.lineAt(toA, QueryType.ByPosNoHeight, oracle, 0, 0);
      toB += end.to - toA;
      toA = end.to;
      while (i > 0 && start.from <= changes[i - 1].toA) {
        fromA = changes[i - 1].fromA;
        fromB = changes[i - 1].fromB;
        i--;
        if (fromA < start.from)
          start = me.lineAt(fromA, QueryType.ByPosNoHeight, oracle, 0, 0);
      }
      fromB += start.from - fromA;
      fromA = start.from;
      let nodes = NodeBuilder.build(oracle.setDoc(doc2), decorations2, fromB, toB);
      me = replace(me, me.replace(fromA, toA, nodes));
    }
    return me.updateHeight(oracle, 0);
  }
  static empty() {
    return new HeightMapText(0, 0, 0);
  }
  // nodes uses null values to indicate the position of line breaks.
  // There are never line breaks at the start or end of the array, or
  // two line breaks next to each other, and the array isn't allowed
  // to be empty (same restrictions as return value from the builder).
  static of(nodes) {
    if (nodes.length == 1)
      return nodes[0];
    let i = 0, j = nodes.length, before = 0, after = 0;
    for (; ; ) {
      if (i == j) {
        if (before > after * 2) {
          let split = nodes[i - 1];
          if (split.break)
            nodes.splice(--i, 1, split.left, null, split.right);
          else
            nodes.splice(--i, 1, split.left, split.right);
          j += 1 + split.break;
          before -= split.size;
        } else if (after > before * 2) {
          let split = nodes[j];
          if (split.break)
            nodes.splice(j, 1, split.left, null, split.right);
          else
            nodes.splice(j, 1, split.left, split.right);
          j += 2 + split.break;
          after -= split.size;
        } else {
          break;
        }
      } else if (before < after) {
        let next = nodes[i++];
        if (next)
          before += next.size;
      } else {
        let next = nodes[--j];
        if (next)
          after += next.size;
      }
    }
    let brk = 0;
    if (nodes[i - 1] == null) {
      brk = 1;
      i--;
    } else if (nodes[i] == null) {
      brk = 1;
      j++;
    }
    return new HeightMapBranch(_HeightMap.of(nodes.slice(0, i)), brk, _HeightMap.of(nodes.slice(j)));
  }
};
function replace(old, val) {
  if (old == val)
    return old;
  if (old.constructor != val.constructor)
    heightChangeFlag = true;
  return val;
}
HeightMap.prototype.size = 1;
var SpaceDeco = /* @__PURE__ */ Decoration.replace({});
var HeightMapBlock = class extends HeightMap {
  constructor(length, height, deco) {
    super(length, height);
    this.deco = deco;
    this.spaceAbove = 0;
  }
  mainBlock(top2, offset) {
    return new BlockInfo(offset, this.length, top2 + this.spaceAbove, this.height - this.spaceAbove, this.deco || 0);
  }
  blockAt(height, _oracle, top2, offset) {
    return this.spaceAbove && height < top2 + this.spaceAbove ? new BlockInfo(offset, 0, top2, this.spaceAbove, SpaceDeco) : this.mainBlock(top2, offset);
  }
  lineAt(_value, _type, oracle, top2, offset) {
    let main = this.mainBlock(top2, offset);
    return this.spaceAbove ? this.blockAt(0, oracle, top2, offset).join(main) : main;
  }
  forEachLine(from, to, oracle, top2, offset, f) {
    if (from <= offset + this.length && to >= offset)
      f(this.lineAt(0, QueryType.ByPos, oracle, top2, offset));
  }
  setMeasuredHeight(measured) {
    let next = measured.heights[measured.index++];
    if (next < 0) {
      this.spaceAbove = -next;
      next = measured.heights[measured.index++];
    } else {
      this.spaceAbove = 0;
    }
    this.setHeight(next);
  }
  updateHeight(oracle, offset = 0, _force = false, measured) {
    if (measured && measured.from <= offset && measured.more)
      this.setMeasuredHeight(measured);
    this.outdated = false;
    return this;
  }
  toString() {
    return `block(${this.length})`;
  }
};
var HeightMapText = class _HeightMapText extends HeightMapBlock {
  constructor(length, height, above) {
    super(length, height, null);
    this.collapsed = 0;
    this.widgetHeight = 0;
    this.breaks = 0;
    this.spaceAbove = above;
  }
  mainBlock(top2, offset) {
    return new BlockInfo(offset, this.length, top2 + this.spaceAbove, this.height - this.spaceAbove, this.breaks);
  }
  replace(_from, _to, nodes) {
    let node = nodes[0];
    if (nodes.length == 1 && (node instanceof _HeightMapText || node instanceof HeightMapGap && node.flags & 4) && Math.abs(this.length - node.length) < 10) {
      if (node instanceof HeightMapGap)
        node = new _HeightMapText(node.length, this.height, this.spaceAbove);
      else
        node.height = this.height;
      if (!this.outdated)
        node.outdated = false;
      return node;
    } else {
      return HeightMap.of(nodes);
    }
  }
  updateHeight(oracle, offset = 0, force = false, measured) {
    if (measured && measured.from <= offset && measured.more) {
      this.setMeasuredHeight(measured);
    } else if (force || this.outdated) {
      this.spaceAbove = 0;
      this.setHeight(Math.max(this.widgetHeight, oracle.heightForLine(this.length - this.collapsed)) + this.breaks * oracle.lineHeight);
    }
    this.outdated = false;
    return this;
  }
  toString() {
    return `line(${this.length}${this.collapsed ? -this.collapsed : ""}${this.widgetHeight ? ":" + this.widgetHeight : ""})`;
  }
};
var HeightMapGap = class _HeightMapGap extends HeightMap {
  constructor(length) {
    super(length, 0);
  }
  heightMetrics(oracle, offset) {
    let firstLine = oracle.doc.lineAt(offset).number, lastLine = oracle.doc.lineAt(offset + this.length).number;
    let lines = lastLine - firstLine + 1;
    let perLine, perChar = 0;
    if (oracle.lineWrapping) {
      let totalPerLine = Math.min(this.height, oracle.lineHeight * lines);
      perLine = totalPerLine / lines;
      if (this.length > lines + 1)
        perChar = (this.height - totalPerLine) / (this.length - lines - 1);
    } else {
      perLine = this.height / lines;
    }
    return { firstLine, lastLine, perLine, perChar };
  }
  blockAt(height, oracle, top2, offset) {
    let { firstLine, lastLine, perLine, perChar } = this.heightMetrics(oracle, offset);
    if (oracle.lineWrapping) {
      let guess = offset + (height < oracle.lineHeight ? 0 : Math.round(Math.max(0, Math.min(1, (height - top2) / this.height)) * this.length));
      let line = oracle.doc.lineAt(guess), lineHeight = perLine + line.length * perChar;
      let lineTop = Math.max(top2, height - lineHeight / 2);
      return new BlockInfo(line.from, line.length, lineTop, lineHeight, 0);
    } else {
      let line = Math.max(0, Math.min(lastLine - firstLine, Math.floor((height - top2) / perLine)));
      let { from, length } = oracle.doc.line(firstLine + line);
      return new BlockInfo(from, length, top2 + perLine * line, perLine, 0);
    }
  }
  lineAt(value, type, oracle, top2, offset) {
    if (type == QueryType.ByHeight)
      return this.blockAt(value, oracle, top2, offset);
    if (type == QueryType.ByPosNoHeight) {
      let { from, to } = oracle.doc.lineAt(value);
      return new BlockInfo(from, to - from, 0, 0, 0);
    }
    let { firstLine, perLine, perChar } = this.heightMetrics(oracle, offset);
    let line = oracle.doc.lineAt(value), lineHeight = perLine + line.length * perChar;
    let linesAbove = line.number - firstLine;
    let lineTop = top2 + perLine * linesAbove + perChar * (line.from - offset - linesAbove);
    return new BlockInfo(line.from, line.length, Math.max(top2, Math.min(lineTop, top2 + this.height - lineHeight)), lineHeight, 0);
  }
  forEachLine(from, to, oracle, top2, offset, f) {
    from = Math.max(from, offset);
    to = Math.min(to, offset + this.length);
    let { firstLine, perLine, perChar } = this.heightMetrics(oracle, offset);
    for (let pos = from, lineTop = top2; pos <= to; ) {
      let line = oracle.doc.lineAt(pos);
      if (pos == from) {
        let linesAbove = line.number - firstLine;
        lineTop += perLine * linesAbove + perChar * (from - offset - linesAbove);
      }
      let lineHeight = perLine + perChar * line.length;
      f(new BlockInfo(line.from, line.length, lineTop, lineHeight, 0));
      lineTop += lineHeight;
      pos = line.to + 1;
    }
  }
  replace(from, to, nodes) {
    let after = this.length - to;
    if (after > 0) {
      let last = nodes[nodes.length - 1];
      if (last instanceof _HeightMapGap)
        nodes[nodes.length - 1] = new _HeightMapGap(last.length + after);
      else
        nodes.push(null, new _HeightMapGap(after - 1));
    }
    if (from > 0) {
      let first = nodes[0];
      if (first instanceof _HeightMapGap)
        nodes[0] = new _HeightMapGap(from + first.length);
      else
        nodes.unshift(new _HeightMapGap(from - 1), null);
    }
    return HeightMap.of(nodes);
  }
  decomposeLeft(to, result) {
    result.push(new _HeightMapGap(to - 1), null);
  }
  decomposeRight(from, result) {
    result.push(null, new _HeightMapGap(this.length - from - 1));
  }
  updateHeight(oracle, offset = 0, force = false, measured) {
    let end = offset + this.length;
    if (measured && measured.from <= offset + this.length && measured.more) {
      let nodes = [], pos = Math.max(offset, measured.from), singleHeight = -1;
      if (measured.from > offset)
        nodes.push(new _HeightMapGap(measured.from - offset - 1).updateHeight(oracle, offset));
      while (pos <= end && measured.more) {
        let len = oracle.doc.lineAt(pos).length;
        if (nodes.length)
          nodes.push(null);
        let height = measured.heights[measured.index++], above = 0;
        if (height < 0) {
          above = -height;
          height = measured.heights[measured.index++];
        }
        if (singleHeight == -1)
          singleHeight = height;
        else if (Math.abs(height - singleHeight) >= Epsilon)
          singleHeight = -2;
        let line = new HeightMapText(len, height, above);
        line.outdated = false;
        nodes.push(line);
        pos += len + 1;
      }
      if (pos <= end)
        nodes.push(null, new _HeightMapGap(end - pos).updateHeight(oracle, pos));
      let result = HeightMap.of(nodes);
      if (singleHeight < 0 || Math.abs(result.height - this.height) >= Epsilon || Math.abs(singleHeight - this.heightMetrics(oracle, offset).perLine) >= Epsilon)
        heightChangeFlag = true;
      return replace(this, result);
    } else if (force || this.outdated) {
      this.setHeight(oracle.heightForGap(offset, offset + this.length));
      this.outdated = false;
    }
    return this;
  }
  toString() {
    return `gap(${this.length})`;
  }
};
var HeightMapBranch = class extends HeightMap {
  constructor(left, brk, right) {
    super(left.length + brk + right.length, left.height + right.height, brk | (left.outdated || right.outdated ? 2 : 0));
    this.left = left;
    this.right = right;
    this.size = left.size + right.size;
  }
  get break() {
    return this.flags & 1;
  }
  blockAt(height, oracle, top2, offset) {
    let mid = top2 + this.left.height;
    return height < mid ? this.left.blockAt(height, oracle, top2, offset) : this.right.blockAt(height, oracle, mid, offset + this.left.length + this.break);
  }
  lineAt(value, type, oracle, top2, offset) {
    let rightTop = top2 + this.left.height, rightOffset = offset + this.left.length + this.break;
    let left = type == QueryType.ByHeight ? value < rightTop : value < rightOffset;
    let base2 = left ? this.left.lineAt(value, type, oracle, top2, offset) : this.right.lineAt(value, type, oracle, rightTop, rightOffset);
    if (this.break || (left ? base2.to < rightOffset : base2.from > rightOffset))
      return base2;
    let subQuery = type == QueryType.ByPosNoHeight ? QueryType.ByPosNoHeight : QueryType.ByPos;
    if (left)
      return base2.join(this.right.lineAt(rightOffset, subQuery, oracle, rightTop, rightOffset));
    else
      return this.left.lineAt(rightOffset, subQuery, oracle, top2, offset).join(base2);
  }
  forEachLine(from, to, oracle, top2, offset, f) {
    let rightTop = top2 + this.left.height, rightOffset = offset + this.left.length + this.break;
    if (this.break) {
      if (from < rightOffset)
        this.left.forEachLine(from, to, oracle, top2, offset, f);
      if (to >= rightOffset)
        this.right.forEachLine(from, to, oracle, rightTop, rightOffset, f);
    } else {
      let mid = this.lineAt(rightOffset, QueryType.ByPos, oracle, top2, offset);
      if (from < mid.from)
        this.left.forEachLine(from, mid.from - 1, oracle, top2, offset, f);
      if (mid.to >= from && mid.from <= to)
        f(mid);
      if (to > mid.to)
        this.right.forEachLine(mid.to + 1, to, oracle, rightTop, rightOffset, f);
    }
  }
  replace(from, to, nodes) {
    let rightStart = this.left.length + this.break;
    if (to < rightStart)
      return this.balanced(this.left.replace(from, to, nodes), this.right);
    if (from > this.left.length)
      return this.balanced(this.left, this.right.replace(from - rightStart, to - rightStart, nodes));
    let result = [];
    if (from > 0)
      this.decomposeLeft(from, result);
    let left = result.length;
    for (let node of nodes)
      result.push(node);
    if (from > 0)
      mergeGaps(result, left - 1);
    if (to < this.length) {
      let right = result.length;
      this.decomposeRight(to, result);
      mergeGaps(result, right);
    }
    return HeightMap.of(result);
  }
  decomposeLeft(to, result) {
    let left = this.left.length;
    if (to <= left)
      return this.left.decomposeLeft(to, result);
    result.push(this.left);
    if (this.break) {
      left++;
      if (to >= left)
        result.push(null);
    }
    if (to > left)
      this.right.decomposeLeft(to - left, result);
  }
  decomposeRight(from, result) {
    let left = this.left.length, right = left + this.break;
    if (from >= right)
      return this.right.decomposeRight(from - right, result);
    if (from < left)
      this.left.decomposeRight(from, result);
    if (this.break && from < right)
      result.push(null);
    result.push(this.right);
  }
  balanced(left, right) {
    if (left.size > 2 * right.size || right.size > 2 * left.size)
      return HeightMap.of(this.break ? [left, null, right] : [left, right]);
    this.left = replace(this.left, left);
    this.right = replace(this.right, right);
    this.setHeight(left.height + right.height);
    this.outdated = left.outdated || right.outdated;
    this.size = left.size + right.size;
    this.length = left.length + this.break + right.length;
    return this;
  }
  updateHeight(oracle, offset = 0, force = false, measured) {
    let { left, right } = this, rightStart = offset + left.length + this.break, rebalance = null;
    if (measured && measured.from <= offset + left.length && measured.more)
      rebalance = left = left.updateHeight(oracle, offset, force, measured);
    else
      left.updateHeight(oracle, offset, force);
    if (measured && measured.from <= rightStart + right.length && measured.more)
      rebalance = right = right.updateHeight(oracle, rightStart, force, measured);
    else
      right.updateHeight(oracle, rightStart, force);
    if (rebalance)
      return this.balanced(left, right);
    this.height = this.left.height + this.right.height;
    this.outdated = false;
    return this;
  }
  toString() {
    return this.left + (this.break ? " " : "-") + this.right;
  }
};
function mergeGaps(nodes, around) {
  let before, after;
  if (nodes[around] == null && (before = nodes[around - 1]) instanceof HeightMapGap && (after = nodes[around + 1]) instanceof HeightMapGap)
    nodes.splice(around - 1, 3, new HeightMapGap(before.length + 1 + after.length));
}
var relevantWidgetHeight = 5;
var NodeBuilder = class _NodeBuilder {
  constructor(pos, oracle) {
    this.pos = pos;
    this.oracle = oracle;
    this.nodes = [];
    this.lineStart = -1;
    this.lineEnd = -1;
    this.covering = null;
    this.writtenTo = pos;
  }
  get isCovered() {
    return this.covering && this.nodes[this.nodes.length - 1] == this.covering;
  }
  span(_from, to) {
    if (this.lineStart > -1) {
      let end = Math.min(to, this.lineEnd), last = this.nodes[this.nodes.length - 1];
      if (last instanceof HeightMapText)
        last.length += end - this.pos;
      else if (end > this.pos || !this.isCovered)
        this.nodes.push(new HeightMapText(end - this.pos, -1, 0));
      this.writtenTo = end;
      if (to > end) {
        this.nodes.push(null);
        this.writtenTo++;
        this.lineStart = -1;
      }
    }
    this.pos = to;
  }
  point(from, to, deco) {
    if (from < to || deco.heightRelevant) {
      let height = deco.widget ? deco.widget.estimatedHeight : 0;
      let breaks = deco.widget ? deco.widget.lineBreaks : 0;
      if (height < 0)
        height = this.oracle.lineHeight;
      let len = to - from;
      if (deco.block) {
        this.addBlock(new HeightMapBlock(len, height, deco));
      } else if (len || breaks || height >= relevantWidgetHeight) {
        this.addLineDeco(height, breaks, len);
      }
    } else if (to > from) {
      this.span(from, to);
    }
    if (this.lineEnd > -1 && this.lineEnd < this.pos)
      this.lineEnd = this.oracle.doc.lineAt(this.pos).to;
  }
  enterLine() {
    if (this.lineStart > -1)
      return;
    let { from, to } = this.oracle.doc.lineAt(this.pos);
    this.lineStart = from;
    this.lineEnd = to;
    if (this.writtenTo < from) {
      if (this.writtenTo < from - 1 || this.nodes[this.nodes.length - 1] == null)
        this.nodes.push(this.blankContent(this.writtenTo, from - 1));
      this.nodes.push(null);
    }
    if (this.pos > from)
      this.nodes.push(new HeightMapText(this.pos - from, -1, 0));
    this.writtenTo = this.pos;
  }
  blankContent(from, to) {
    let gap = new HeightMapGap(to - from);
    if (this.oracle.doc.lineAt(from).to == to)
      gap.flags |= 4;
    return gap;
  }
  ensureLine() {
    this.enterLine();
    let last = this.nodes.length ? this.nodes[this.nodes.length - 1] : null;
    if (last instanceof HeightMapText)
      return last;
    let line = new HeightMapText(0, -1, 0);
    this.nodes.push(line);
    return line;
  }
  addBlock(block) {
    this.enterLine();
    let deco = block.deco;
    if (deco && deco.startSide > 0 && !this.isCovered)
      this.ensureLine();
    this.nodes.push(block);
    this.writtenTo = this.pos = this.pos + block.length;
    if (deco && deco.endSide > 0)
      this.covering = block;
  }
  addLineDeco(height, breaks, length) {
    let line = this.ensureLine();
    line.length += length;
    line.collapsed += length;
    line.widgetHeight = Math.max(line.widgetHeight, height);
    line.breaks += breaks;
    this.writtenTo = this.pos = this.pos + length;
  }
  finish(from) {
    let last = this.nodes.length == 0 ? null : this.nodes[this.nodes.length - 1];
    if (this.lineStart > -1 && !(last instanceof HeightMapText) && !this.isCovered)
      this.nodes.push(new HeightMapText(0, -1, 0));
    else if (this.writtenTo < this.pos || last == null)
      this.nodes.push(this.blankContent(this.writtenTo, this.pos));
    let pos = from;
    for (let node of this.nodes) {
      if (node instanceof HeightMapText)
        node.updateHeight(this.oracle, pos);
      pos += node ? node.length : 1;
    }
    return this.nodes;
  }
  // Always called with a region that on both sides either stretches
  // to a line break or the end of the document.
  // The returned array uses null to indicate line breaks, but never
  // starts or ends in a line break, or has multiple line breaks next
  // to each other.
  static build(oracle, decorations2, from, to) {
    let builder = new _NodeBuilder(from, oracle);
    RangeSet.spans(decorations2, from, to, builder, 0);
    return builder.finish(from);
  }
};
function heightRelevantDecoChanges(a2, b, diff) {
  let comp = new DecorationComparator2();
  RangeSet.compare(a2, b, diff, comp, 0);
  return comp.changes;
}
var DecorationComparator2 = class {
  constructor() {
    this.changes = [];
  }
  compareRange() {
  }
  comparePoint(from, to, a2, b) {
    if (from < to || a2 && a2.heightRelevant || b && b.heightRelevant)
      addRange(from, to, this.changes, 5);
  }
};
function visiblePixelRange(dom, paddingTop) {
  let rect = dom.getBoundingClientRect();
  let doc2 = dom.ownerDocument, win = doc2.defaultView || window;
  let left = Math.max(0, rect.left), right = Math.min(win.innerWidth, rect.right);
  let top2 = Math.max(0, rect.top), bottom = Math.min(win.innerHeight, rect.bottom);
  for (let parent = dom.parentNode; parent && parent != doc2.body; ) {
    if (parent.nodeType == 1) {
      let elt = parent;
      let style = window.getComputedStyle(elt);
      if ((elt.scrollHeight > elt.clientHeight || elt.scrollWidth > elt.clientWidth) && style.overflow != "visible") {
        let parentRect = elt.getBoundingClientRect();
        left = Math.max(left, parentRect.left);
        right = Math.min(right, parentRect.right);
        top2 = Math.max(top2, parentRect.top);
        bottom = Math.min(parent == dom.parentNode ? win.innerHeight : bottom, parentRect.bottom);
      }
      parent = style.position == "absolute" || style.position == "fixed" ? elt.offsetParent : elt.parentNode;
    } else if (parent.nodeType == 11) {
      parent = parent.host;
    } else {
      break;
    }
  }
  return {
    left: left - rect.left,
    right: Math.max(left, right) - rect.left,
    top: top2 - (rect.top + paddingTop),
    bottom: Math.max(top2, bottom) - (rect.top + paddingTop)
  };
}
function inWindow(elt) {
  let rect = elt.getBoundingClientRect(), win = elt.ownerDocument.defaultView || window;
  return rect.left < win.innerWidth && rect.right > 0 && rect.top < win.innerHeight && rect.bottom > 0;
}
function fullPixelRange(dom, paddingTop) {
  let rect = dom.getBoundingClientRect();
  return {
    left: 0,
    right: rect.right - rect.left,
    top: paddingTop,
    bottom: rect.bottom - (rect.top + paddingTop)
  };
}
var LineGap = class {
  constructor(from, to, size, displaySize) {
    this.from = from;
    this.to = to;
    this.size = size;
    this.displaySize = displaySize;
  }
  static same(a2, b) {
    if (a2.length != b.length)
      return false;
    for (let i = 0; i < a2.length; i++) {
      let gA = a2[i], gB = b[i];
      if (gA.from != gB.from || gA.to != gB.to || gA.size != gB.size)
        return false;
    }
    return true;
  }
  draw(viewState, wrapping) {
    return Decoration.replace({
      widget: new LineGapWidget(this.displaySize * (wrapping ? viewState.scaleY : viewState.scaleX), wrapping)
    }).range(this.from, this.to);
  }
};
var LineGapWidget = class extends WidgetType {
  constructor(size, vertical) {
    super();
    this.size = size;
    this.vertical = vertical;
  }
  eq(other) {
    return other.size == this.size && other.vertical == this.vertical;
  }
  toDOM() {
    let elt = document.createElement("div");
    if (this.vertical) {
      elt.style.height = this.size + "px";
    } else {
      elt.style.width = this.size + "px";
      elt.style.height = "2px";
      elt.style.display = "inline-block";
    }
    return elt;
  }
  get estimatedHeight() {
    return this.vertical ? this.size : -1;
  }
};
var ViewState = class {
  constructor(state) {
    this.state = state;
    this.pixelViewport = { left: 0, right: window.innerWidth, top: 0, bottom: 0 };
    this.inView = true;
    this.paddingTop = 0;
    this.paddingBottom = 0;
    this.contentDOMWidth = 0;
    this.contentDOMHeight = 0;
    this.editorHeight = 0;
    this.editorWidth = 0;
    this.scrollTop = 0;
    this.scrolledToBottom = false;
    this.scaleX = 1;
    this.scaleY = 1;
    this.scrollAnchorPos = 0;
    this.scrollAnchorHeight = -1;
    this.scaler = IdScaler;
    this.scrollTarget = null;
    this.printing = false;
    this.mustMeasureContent = true;
    this.defaultTextDirection = Direction.LTR;
    this.visibleRanges = [];
    this.mustEnforceCursorAssoc = false;
    let guessWrapping = state.facet(contentAttributes).some((v) => typeof v != "function" && v.class == "cm-lineWrapping");
    this.heightOracle = new HeightOracle(guessWrapping);
    this.stateDeco = state.facet(decorations).filter((d) => typeof d != "function");
    this.heightMap = HeightMap.empty().applyChanges(this.stateDeco, Text.empty, this.heightOracle.setDoc(state.doc), [new ChangedRange(0, 0, 0, state.doc.length)]);
    for (let i = 0; i < 2; i++) {
      this.viewport = this.getViewport(0, null);
      if (!this.updateForViewport())
        break;
    }
    this.updateViewportLines();
    this.lineGaps = this.ensureLineGaps([]);
    this.lineGapDeco = Decoration.set(this.lineGaps.map((gap) => gap.draw(this, false)));
    this.computeVisibleRanges();
  }
  updateForViewport() {
    let viewports = [this.viewport], { main } = this.state.selection;
    for (let i = 0; i <= 1; i++) {
      let pos = i ? main.head : main.anchor;
      if (!viewports.some(({ from, to }) => pos >= from && pos <= to)) {
        let { from, to } = this.lineBlockAt(pos);
        viewports.push(new Viewport(from, to));
      }
    }
    this.viewports = viewports.sort((a2, b) => a2.from - b.from);
    return this.updateScaler();
  }
  updateScaler() {
    let scaler = this.scaler;
    this.scaler = this.heightMap.height <= 7e6 ? IdScaler : new BigScaler(this.heightOracle, this.heightMap, this.viewports);
    return scaler.eq(this.scaler) ? 0 : 2;
  }
  updateViewportLines() {
    this.viewportLines = [];
    this.heightMap.forEachLine(this.viewport.from, this.viewport.to, this.heightOracle.setDoc(this.state.doc), 0, 0, (block) => {
      this.viewportLines.push(scaleBlock(block, this.scaler));
    });
  }
  update(update, scrollTarget = null) {
    this.state = update.state;
    let prevDeco = this.stateDeco;
    this.stateDeco = this.state.facet(decorations).filter((d) => typeof d != "function");
    let contentChanges = update.changedRanges;
    let heightChanges = ChangedRange.extendWithRanges(contentChanges, heightRelevantDecoChanges(prevDeco, this.stateDeco, update ? update.changes : ChangeSet.empty(this.state.doc.length)));
    let prevHeight = this.heightMap.height;
    let scrollAnchor = this.scrolledToBottom ? null : this.scrollAnchorAt(this.scrollTop);
    clearHeightChangeFlag();
    this.heightMap = this.heightMap.applyChanges(this.stateDeco, update.startState.doc, this.heightOracle.setDoc(this.state.doc), heightChanges);
    if (this.heightMap.height != prevHeight || heightChangeFlag)
      update.flags |= 2;
    if (scrollAnchor) {
      this.scrollAnchorPos = update.changes.mapPos(scrollAnchor.from, -1);
      this.scrollAnchorHeight = scrollAnchor.top;
    } else {
      this.scrollAnchorPos = -1;
      this.scrollAnchorHeight = prevHeight;
    }
    let viewport = heightChanges.length ? this.mapViewport(this.viewport, update.changes) : this.viewport;
    if (scrollTarget && (scrollTarget.range.head < viewport.from || scrollTarget.range.head > viewport.to) || !this.viewportIsAppropriate(viewport))
      viewport = this.getViewport(0, scrollTarget);
    let viewportChange = viewport.from != this.viewport.from || viewport.to != this.viewport.to;
    this.viewport = viewport;
    update.flags |= this.updateForViewport();
    if (viewportChange || !update.changes.empty || update.flags & 2)
      this.updateViewportLines();
    if (this.lineGaps.length || this.viewport.to - this.viewport.from > 2e3 << 1)
      this.updateLineGaps(this.ensureLineGaps(this.mapLineGaps(this.lineGaps, update.changes)));
    update.flags |= this.computeVisibleRanges(update.changes);
    if (scrollTarget)
      this.scrollTarget = scrollTarget;
    if (!this.mustEnforceCursorAssoc && update.selectionSet && update.view.lineWrapping && update.state.selection.main.empty && update.state.selection.main.assoc && !update.state.facet(nativeSelectionHidden))
      this.mustEnforceCursorAssoc = true;
  }
  measure(view) {
    let dom = view.contentDOM, style = window.getComputedStyle(dom);
    let oracle = this.heightOracle;
    let whiteSpace = style.whiteSpace;
    this.defaultTextDirection = style.direction == "rtl" ? Direction.RTL : Direction.LTR;
    let refresh = this.heightOracle.mustRefreshForWrapping(whiteSpace);
    let domRect = dom.getBoundingClientRect();
    let measureContent = refresh || this.mustMeasureContent || this.contentDOMHeight != domRect.height;
    this.contentDOMHeight = domRect.height;
    this.mustMeasureContent = false;
    let result = 0, bias = 0;
    if (domRect.width && domRect.height) {
      let { scaleX, scaleY } = getScale(dom, domRect);
      if (scaleX > 5e-3 && Math.abs(this.scaleX - scaleX) > 5e-3 || scaleY > 5e-3 && Math.abs(this.scaleY - scaleY) > 5e-3) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        result |= 16;
        refresh = measureContent = true;
      }
    }
    let paddingTop = (parseInt(style.paddingTop) || 0) * this.scaleY;
    let paddingBottom = (parseInt(style.paddingBottom) || 0) * this.scaleY;
    if (this.paddingTop != paddingTop || this.paddingBottom != paddingBottom) {
      this.paddingTop = paddingTop;
      this.paddingBottom = paddingBottom;
      result |= 16 | 2;
    }
    if (this.editorWidth != view.scrollDOM.clientWidth) {
      if (oracle.lineWrapping)
        measureContent = true;
      this.editorWidth = view.scrollDOM.clientWidth;
      result |= 16;
    }
    let scrollTop = view.scrollDOM.scrollTop * this.scaleY;
    if (this.scrollTop != scrollTop) {
      this.scrollAnchorHeight = -1;
      this.scrollTop = scrollTop;
    }
    this.scrolledToBottom = isScrolledToBottom(view.scrollDOM);
    let pixelViewport = (this.printing ? fullPixelRange : visiblePixelRange)(dom, this.paddingTop);
    let dTop = pixelViewport.top - this.pixelViewport.top, dBottom = pixelViewport.bottom - this.pixelViewport.bottom;
    this.pixelViewport = pixelViewport;
    let inView = this.pixelViewport.bottom > this.pixelViewport.top && this.pixelViewport.right > this.pixelViewport.left;
    if (inView != this.inView) {
      this.inView = inView;
      if (inView)
        measureContent = true;
    }
    if (!this.inView && !this.scrollTarget && !inWindow(view.dom))
      return 0;
    let contentWidth = domRect.width;
    if (this.contentDOMWidth != contentWidth || this.editorHeight != view.scrollDOM.clientHeight) {
      this.contentDOMWidth = domRect.width;
      this.editorHeight = view.scrollDOM.clientHeight;
      result |= 16;
    }
    if (measureContent) {
      let lineHeights = view.docView.measureVisibleLineHeights(this.viewport);
      if (oracle.mustRefreshForHeights(lineHeights))
        refresh = true;
      if (refresh || oracle.lineWrapping && Math.abs(contentWidth - this.contentDOMWidth) > oracle.charWidth) {
        let { lineHeight, charWidth, textHeight } = view.docView.measureTextSize();
        refresh = lineHeight > 0 && oracle.refresh(whiteSpace, lineHeight, charWidth, textHeight, Math.max(5, contentWidth / charWidth), lineHeights);
        if (refresh) {
          view.docView.minWidth = 0;
          result |= 16;
        }
      }
      if (dTop > 0 && dBottom > 0)
        bias = Math.max(dTop, dBottom);
      else if (dTop < 0 && dBottom < 0)
        bias = Math.min(dTop, dBottom);
      clearHeightChangeFlag();
      for (let vp of this.viewports) {
        let heights = vp.from == this.viewport.from ? lineHeights : view.docView.measureVisibleLineHeights(vp);
        this.heightMap = (refresh ? HeightMap.empty().applyChanges(this.stateDeco, Text.empty, this.heightOracle, [new ChangedRange(0, 0, 0, view.state.doc.length)]) : this.heightMap).updateHeight(oracle, 0, refresh, new MeasuredHeights(vp.from, heights));
      }
      if (heightChangeFlag)
        result |= 2;
    }
    let viewportChange = !this.viewportIsAppropriate(this.viewport, bias) || this.scrollTarget && (this.scrollTarget.range.head < this.viewport.from || this.scrollTarget.range.head > this.viewport.to);
    if (viewportChange) {
      if (result & 2)
        result |= this.updateScaler();
      this.viewport = this.getViewport(bias, this.scrollTarget);
      result |= this.updateForViewport();
    }
    if (result & 2 || viewportChange)
      this.updateViewportLines();
    if (this.lineGaps.length || this.viewport.to - this.viewport.from > 2e3 << 1)
      this.updateLineGaps(this.ensureLineGaps(refresh ? [] : this.lineGaps, view));
    result |= this.computeVisibleRanges();
    if (this.mustEnforceCursorAssoc) {
      this.mustEnforceCursorAssoc = false;
      view.docView.enforceCursorAssoc();
    }
    return result;
  }
  get visibleTop() {
    return this.scaler.fromDOM(this.pixelViewport.top);
  }
  get visibleBottom() {
    return this.scaler.fromDOM(this.pixelViewport.bottom);
  }
  getViewport(bias, scrollTarget) {
    let marginTop = 0.5 - Math.max(-0.5, Math.min(0.5, bias / 1e3 / 2));
    let map = this.heightMap, oracle = this.heightOracle;
    let { visibleTop, visibleBottom } = this;
    let viewport = new Viewport(map.lineAt(visibleTop - marginTop * 1e3, QueryType.ByHeight, oracle, 0, 0).from, map.lineAt(visibleBottom + (1 - marginTop) * 1e3, QueryType.ByHeight, oracle, 0, 0).to);
    if (scrollTarget) {
      let { head } = scrollTarget.range;
      if (head < viewport.from || head > viewport.to) {
        let viewHeight = Math.min(this.editorHeight, this.pixelViewport.bottom - this.pixelViewport.top);
        let block = map.lineAt(head, QueryType.ByPos, oracle, 0, 0), topPos;
        if (scrollTarget.y == "center")
          topPos = (block.top + block.bottom) / 2 - viewHeight / 2;
        else if (scrollTarget.y == "start" || scrollTarget.y == "nearest" && head < viewport.from)
          topPos = block.top;
        else
          topPos = block.bottom - viewHeight;
        viewport = new Viewport(map.lineAt(topPos - 1e3 / 2, QueryType.ByHeight, oracle, 0, 0).from, map.lineAt(topPos + viewHeight + 1e3 / 2, QueryType.ByHeight, oracle, 0, 0).to);
      }
    }
    return viewport;
  }
  mapViewport(viewport, changes) {
    let from = changes.mapPos(viewport.from, -1), to = changes.mapPos(viewport.to, 1);
    return new Viewport(this.heightMap.lineAt(from, QueryType.ByPos, this.heightOracle, 0, 0).from, this.heightMap.lineAt(to, QueryType.ByPos, this.heightOracle, 0, 0).to);
  }
  // Checks if a given viewport covers the visible part of the
  // document and not too much beyond that.
  viewportIsAppropriate({ from, to }, bias = 0) {
    if (!this.inView)
      return true;
    let { top: top2 } = this.heightMap.lineAt(from, QueryType.ByPos, this.heightOracle, 0, 0);
    let { bottom } = this.heightMap.lineAt(to, QueryType.ByPos, this.heightOracle, 0, 0);
    let { visibleTop, visibleBottom } = this;
    return (from == 0 || top2 <= visibleTop - Math.max(10, Math.min(
      -bias,
      250
      /* VP.MaxCoverMargin */
    ))) && (to == this.state.doc.length || bottom >= visibleBottom + Math.max(10, Math.min(
      bias,
      250
      /* VP.MaxCoverMargin */
    ))) && (top2 > visibleTop - 2 * 1e3 && bottom < visibleBottom + 2 * 1e3);
  }
  mapLineGaps(gaps, changes) {
    if (!gaps.length || changes.empty)
      return gaps;
    let mapped = [];
    for (let gap of gaps)
      if (!changes.touchesRange(gap.from, gap.to))
        mapped.push(new LineGap(changes.mapPos(gap.from), changes.mapPos(gap.to), gap.size, gap.displaySize));
    return mapped;
  }
  // Computes positions in the viewport where the start or end of a
  // line should be hidden, trying to reuse existing line gaps when
  // appropriate to avoid unneccesary redraws.
  // Uses crude character-counting for the positioning and sizing,
  // since actual DOM coordinates aren't always available and
  // predictable. Relies on generous margins (see LG.Margin) to hide
  // the artifacts this might produce from the user.
  ensureLineGaps(current, mayMeasure) {
    let wrapping = this.heightOracle.lineWrapping;
    let margin = wrapping ? 1e4 : 2e3, halfMargin = margin >> 1, doubleMargin = margin << 1;
    if (this.defaultTextDirection != Direction.LTR && !wrapping)
      return [];
    let gaps = [];
    let addGap = (from, to, line, structure) => {
      if (to - from < halfMargin)
        return;
      let sel = this.state.selection.main, avoid = [sel.from];
      if (!sel.empty)
        avoid.push(sel.to);
      for (let pos of avoid) {
        if (pos > from && pos < to) {
          addGap(from, pos - 10, line, structure);
          addGap(pos + 10, to, line, structure);
          return;
        }
      }
      let gap = find(current, (gap2) => gap2.from >= line.from && gap2.to <= line.to && Math.abs(gap2.from - from) < halfMargin && Math.abs(gap2.to - to) < halfMargin && !avoid.some((pos) => gap2.from < pos && gap2.to > pos));
      if (!gap) {
        if (to < line.to && mayMeasure && wrapping && mayMeasure.visibleRanges.some((r) => r.from <= to && r.to >= to)) {
          let lineStart = mayMeasure.moveToLineBoundary(EditorSelection.cursor(to), false, true).head;
          if (lineStart > from)
            to = lineStart;
        }
        let size = this.gapSize(line, from, to, structure);
        let displaySize = wrapping || size < 2e6 ? size : 2e6;
        gap = new LineGap(from, to, size, displaySize);
      }
      gaps.push(gap);
    };
    let checkLine = (line) => {
      if (line.length < doubleMargin || line.type != BlockType.Text)
        return;
      let structure = lineStructure(line.from, line.to, this.stateDeco);
      if (structure.total < doubleMargin)
        return;
      let target = this.scrollTarget ? this.scrollTarget.range.head : null;
      let viewFrom, viewTo;
      if (wrapping) {
        let marginHeight = margin / this.heightOracle.lineLength * this.heightOracle.lineHeight;
        let top2, bot;
        if (target != null) {
          let targetFrac = findFraction(structure, target);
          let spaceFrac = ((this.visibleBottom - this.visibleTop) / 2 + marginHeight) / line.height;
          top2 = targetFrac - spaceFrac;
          bot = targetFrac + spaceFrac;
        } else {
          top2 = (this.visibleTop - line.top - marginHeight) / line.height;
          bot = (this.visibleBottom - line.top + marginHeight) / line.height;
        }
        viewFrom = findPosition(structure, top2);
        viewTo = findPosition(structure, bot);
      } else {
        let totalWidth = structure.total * this.heightOracle.charWidth;
        let marginWidth = margin * this.heightOracle.charWidth;
        let horizOffset = 0;
        if (totalWidth > 2e6)
          for (let old of current) {
            if (old.from >= line.from && old.from < line.to && old.size != old.displaySize && old.from * this.heightOracle.charWidth + horizOffset < this.pixelViewport.left)
              horizOffset = old.size - old.displaySize;
          }
        let pxLeft = this.pixelViewport.left + horizOffset, pxRight = this.pixelViewport.right + horizOffset;
        let left, right;
        if (target != null) {
          let targetFrac = findFraction(structure, target);
          let spaceFrac = ((pxRight - pxLeft) / 2 + marginWidth) / totalWidth;
          left = targetFrac - spaceFrac;
          right = targetFrac + spaceFrac;
        } else {
          left = (pxLeft - marginWidth) / totalWidth;
          right = (pxRight + marginWidth) / totalWidth;
        }
        viewFrom = findPosition(structure, left);
        viewTo = findPosition(structure, right);
      }
      if (viewFrom > line.from)
        addGap(line.from, viewFrom, line, structure);
      if (viewTo < line.to)
        addGap(viewTo, line.to, line, structure);
    };
    for (let line of this.viewportLines) {
      if (Array.isArray(line.type))
        line.type.forEach(checkLine);
      else
        checkLine(line);
    }
    return gaps;
  }
  gapSize(line, from, to, structure) {
    let fraction = findFraction(structure, to) - findFraction(structure, from);
    if (this.heightOracle.lineWrapping) {
      return line.height * fraction;
    } else {
      return structure.total * this.heightOracle.charWidth * fraction;
    }
  }
  updateLineGaps(gaps) {
    if (!LineGap.same(gaps, this.lineGaps)) {
      this.lineGaps = gaps;
      this.lineGapDeco = Decoration.set(gaps.map((gap) => gap.draw(this, this.heightOracle.lineWrapping)));
    }
  }
  computeVisibleRanges(changes) {
    let deco = this.stateDeco;
    if (this.lineGaps.length)
      deco = deco.concat(this.lineGapDeco);
    let ranges = [];
    RangeSet.spans(deco, this.viewport.from, this.viewport.to, {
      span(from, to) {
        ranges.push({ from, to });
      },
      point() {
      }
    }, 20);
    let changed = 0;
    if (ranges.length != this.visibleRanges.length) {
      changed = 8 | 4;
    } else {
      for (let i = 0; i < ranges.length && !(changed & 8); i++) {
        let old = this.visibleRanges[i], nw = ranges[i];
        if (old.from != nw.from || old.to != nw.to) {
          changed |= 4;
          if (!(changes && changes.mapPos(old.from, -1) == nw.from && changes.mapPos(old.to, 1) == nw.to))
            changed |= 8;
        }
      }
    }
    this.visibleRanges = ranges;
    return changed;
  }
  lineBlockAt(pos) {
    return pos >= this.viewport.from && pos <= this.viewport.to && this.viewportLines.find((b) => b.from <= pos && b.to >= pos) || scaleBlock(this.heightMap.lineAt(pos, QueryType.ByPos, this.heightOracle, 0, 0), this.scaler);
  }
  lineBlockAtHeight(height) {
    return height >= this.viewportLines[0].top && height <= this.viewportLines[this.viewportLines.length - 1].bottom && this.viewportLines.find((l) => l.top <= height && l.bottom >= height) || scaleBlock(this.heightMap.lineAt(this.scaler.fromDOM(height), QueryType.ByHeight, this.heightOracle, 0, 0), this.scaler);
  }
  scrollAnchorAt(scrollTop) {
    let block = this.lineBlockAtHeight(scrollTop + 8);
    return block.from >= this.viewport.from || this.viewportLines[0].top - scrollTop > 200 ? block : this.viewportLines[0];
  }
  elementAtHeight(height) {
    return scaleBlock(this.heightMap.blockAt(this.scaler.fromDOM(height), this.heightOracle, 0, 0), this.scaler);
  }
  get docHeight() {
    return this.scaler.toDOM(this.heightMap.height);
  }
  get contentHeight() {
    return this.docHeight + this.paddingTop + this.paddingBottom;
  }
};
var Viewport = class {
  constructor(from, to) {
    this.from = from;
    this.to = to;
  }
};
function lineStructure(from, to, stateDeco) {
  let ranges = [], pos = from, total = 0;
  RangeSet.spans(stateDeco, from, to, {
    span() {
    },
    point(from2, to2) {
      if (from2 > pos) {
        ranges.push({ from: pos, to: from2 });
        total += from2 - pos;
      }
      pos = to2;
    }
  }, 20);
  if (pos < to) {
    ranges.push({ from: pos, to });
    total += to - pos;
  }
  return { total, ranges };
}
function findPosition({ total, ranges }, ratio) {
  if (ratio <= 0)
    return ranges[0].from;
  if (ratio >= 1)
    return ranges[ranges.length - 1].to;
  let dist2 = Math.floor(total * ratio);
  for (let i = 0; ; i++) {
    let { from, to } = ranges[i], size = to - from;
    if (dist2 <= size)
      return from + dist2;
    dist2 -= size;
  }
}
function findFraction(structure, pos) {
  let counted = 0;
  for (let { from, to } of structure.ranges) {
    if (pos <= to) {
      counted += pos - from;
      break;
    }
    counted += to - from;
  }
  return counted / structure.total;
}
function find(array, f) {
  for (let val of array)
    if (f(val))
      return val;
  return void 0;
}
var IdScaler = {
  toDOM(n) {
    return n;
  },
  fromDOM(n) {
    return n;
  },
  scale: 1,
  eq(other) {
    return other == this;
  }
};
var BigScaler = class _BigScaler {
  constructor(oracle, heightMap, viewports) {
    let vpHeight = 0, base2 = 0, domBase = 0;
    this.viewports = viewports.map(({ from, to }) => {
      let top2 = heightMap.lineAt(from, QueryType.ByPos, oracle, 0, 0).top;
      let bottom = heightMap.lineAt(to, QueryType.ByPos, oracle, 0, 0).bottom;
      vpHeight += bottom - top2;
      return { from, to, top: top2, bottom, domTop: 0, domBottom: 0 };
    });
    this.scale = (7e6 - vpHeight) / (heightMap.height - vpHeight);
    for (let obj of this.viewports) {
      obj.domTop = domBase + (obj.top - base2) * this.scale;
      domBase = obj.domBottom = obj.domTop + (obj.bottom - obj.top);
      base2 = obj.bottom;
    }
  }
  toDOM(n) {
    for (let i = 0, base2 = 0, domBase = 0; ; i++) {
      let vp = i < this.viewports.length ? this.viewports[i] : null;
      if (!vp || n < vp.top)
        return domBase + (n - base2) * this.scale;
      if (n <= vp.bottom)
        return vp.domTop + (n - vp.top);
      base2 = vp.bottom;
      domBase = vp.domBottom;
    }
  }
  fromDOM(n) {
    for (let i = 0, base2 = 0, domBase = 0; ; i++) {
      let vp = i < this.viewports.length ? this.viewports[i] : null;
      if (!vp || n < vp.domTop)
        return base2 + (n - domBase) / this.scale;
      if (n <= vp.domBottom)
        return vp.top + (n - vp.domTop);
      base2 = vp.bottom;
      domBase = vp.domBottom;
    }
  }
  eq(other) {
    if (!(other instanceof _BigScaler))
      return false;
    return this.scale == other.scale && this.viewports.length == other.viewports.length && this.viewports.every((vp, i) => vp.from == other.viewports[i].from && vp.to == other.viewports[i].to);
  }
};
function scaleBlock(block, scaler) {
  if (scaler.scale == 1)
    return block;
  let bTop = scaler.toDOM(block.top), bBottom = scaler.toDOM(block.bottom);
  return new BlockInfo(block.from, block.length, bTop, bBottom - bTop, Array.isArray(block._content) ? block._content.map((b) => scaleBlock(b, scaler)) : block._content);
}
var theme = /* @__PURE__ */ Facet.define({ combine: (strs) => strs.join(" ") });
var darkTheme = /* @__PURE__ */ Facet.define({ combine: (values) => values.indexOf(true) > -1 });
var baseThemeID = /* @__PURE__ */ StyleModule.newName();
var baseLightID = /* @__PURE__ */ StyleModule.newName();
var baseDarkID = /* @__PURE__ */ StyleModule.newName();
var lightDarkIDs = { "&light": "." + baseLightID, "&dark": "." + baseDarkID };
function buildTheme(main, spec, scopes) {
  return new StyleModule(spec, {
    finish(sel) {
      return /&/.test(sel) ? sel.replace(/&\w*/, (m) => {
        if (m == "&")
          return main;
        if (!scopes || !scopes[m])
          throw new RangeError(`Unsupported selector: ${m}`);
        return scopes[m];
      }) : main + " " + sel;
    }
  });
}
var baseTheme$1 = /* @__PURE__ */ buildTheme("." + baseThemeID, {
  "&": {
    position: "relative !important",
    boxSizing: "border-box",
    "&.cm-focused": {
      // Provide a simple default outline to make sure a focused
      // editor is visually distinct. Can't leave the default behavior
      // because that will apply to the content element, which is
      // inside the scrollable container and doesn't include the
      // gutters. We also can't use an 'auto' outline, since those
      // are, for some reason, drawn behind the element content, which
      // will cause things like the active line background to cover
      // the outline (#297).
      outline: "1px dotted #212121"
    },
    display: "flex !important",
    flexDirection: "column"
  },
  ".cm-scroller": {
    display: "flex !important",
    alignItems: "flex-start !important",
    fontFamily: "monospace",
    lineHeight: 1.4,
    height: "100%",
    overflowX: "auto",
    position: "relative",
    zIndex: 0,
    overflowAnchor: "none"
  },
  ".cm-content": {
    margin: 0,
    flexGrow: 2,
    flexShrink: 0,
    display: "block",
    whiteSpace: "pre",
    wordWrap: "normal",
    // https://github.com/codemirror/dev/issues/456
    boxSizing: "border-box",
    minHeight: "100%",
    padding: "4px 0",
    outline: "none",
    "&[contenteditable=true]": {
      WebkitUserModify: "read-write-plaintext-only"
    }
  },
  ".cm-lineWrapping": {
    whiteSpace_fallback: "pre-wrap",
    // For IE
    whiteSpace: "break-spaces",
    wordBreak: "break-word",
    // For Safari, which doesn't support overflow-wrap: anywhere
    overflowWrap: "anywhere",
    flexShrink: 1
  },
  "&light .cm-content": { caretColor: "black" },
  "&dark .cm-content": { caretColor: "white" },
  ".cm-line": {
    display: "block",
    padding: "0 2px 0 6px"
  },
  ".cm-layer": {
    position: "absolute",
    left: 0,
    top: 0,
    contain: "size style",
    "& > *": {
      position: "absolute"
    }
  },
  "&light .cm-selectionBackground": {
    background: "#d9d9d9"
  },
  "&dark .cm-selectionBackground": {
    background: "#222"
  },
  "&light.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground": {
    background: "#d7d4f0"
  },
  "&dark.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground": {
    background: "#233"
  },
  ".cm-cursorLayer": {
    pointerEvents: "none"
  },
  "&.cm-focused > .cm-scroller > .cm-cursorLayer": {
    animation: "steps(1) cm-blink 1.2s infinite"
  },
  // Two animations defined so that we can switch between them to
  // restart the animation without forcing another style
  // recomputation.
  "@keyframes cm-blink": { "0%": {}, "50%": { opacity: 0 }, "100%": {} },
  "@keyframes cm-blink2": { "0%": {}, "50%": { opacity: 0 }, "100%": {} },
  ".cm-cursor, .cm-dropCursor": {
    borderLeft: "1.2px solid black",
    marginLeft: "-0.6px",
    pointerEvents: "none"
  },
  ".cm-cursor": {
    display: "none"
  },
  "&dark .cm-cursor": {
    borderLeftColor: "#ddd"
  },
  ".cm-dropCursor": {
    position: "absolute"
  },
  "&.cm-focused > .cm-scroller > .cm-cursorLayer .cm-cursor": {
    display: "block"
  },
  ".cm-iso": {
    unicodeBidi: "isolate"
  },
  ".cm-announced": {
    position: "fixed",
    top: "-10000px"
  },
  "@media print": {
    ".cm-announced": { display: "none" }
  },
  "&light .cm-activeLine": { backgroundColor: "#cceeff44" },
  "&dark .cm-activeLine": { backgroundColor: "#99eeff33" },
  "&light .cm-specialChar": { color: "red" },
  "&dark .cm-specialChar": { color: "#f78" },
  ".cm-gutters": {
    flexShrink: 0,
    display: "flex",
    height: "100%",
    boxSizing: "border-box",
    zIndex: 200
  },
  ".cm-gutters-before": { insetInlineStart: 0 },
  ".cm-gutters-after": { insetInlineEnd: 0 },
  "&light .cm-gutters": {
    backgroundColor: "#f5f5f5",
    color: "#6c6c6c",
    border: "0px solid #ddd",
    "&.cm-gutters-before": { borderRightWidth: "1px" },
    "&.cm-gutters-after": { borderLeftWidth: "1px" }
  },
  "&dark .cm-gutters": {
    backgroundColor: "#333338",
    color: "#ccc"
  },
  ".cm-gutter": {
    display: "flex !important",
    // Necessary -- prevents margin collapsing
    flexDirection: "column",
    flexShrink: 0,
    boxSizing: "border-box",
    minHeight: "100%",
    overflow: "hidden"
  },
  ".cm-gutterElement": {
    boxSizing: "border-box"
  },
  ".cm-lineNumbers .cm-gutterElement": {
    padding: "0 3px 0 5px",
    minWidth: "20px",
    textAlign: "right",
    whiteSpace: "nowrap"
  },
  "&light .cm-activeLineGutter": {
    backgroundColor: "#e2f2ff"
  },
  "&dark .cm-activeLineGutter": {
    backgroundColor: "#222227"
  },
  ".cm-panels": {
    boxSizing: "border-box",
    position: "sticky",
    left: 0,
    right: 0,
    zIndex: 300
  },
  "&light .cm-panels": {
    backgroundColor: "#f5f5f5",
    color: "black"
  },
  "&light .cm-panels-top": {
    borderBottom: "1px solid #ddd"
  },
  "&light .cm-panels-bottom": {
    borderTop: "1px solid #ddd"
  },
  "&dark .cm-panels": {
    backgroundColor: "#333338",
    color: "white"
  },
  ".cm-dialog": {
    padding: "2px 19px 4px 6px",
    position: "relative",
    "& label": { fontSize: "80%" }
  },
  ".cm-dialog-close": {
    position: "absolute",
    top: "3px",
    right: "4px",
    backgroundColor: "inherit",
    border: "none",
    font: "inherit",
    fontSize: "14px",
    padding: "0"
  },
  ".cm-tab": {
    display: "inline-block",
    overflow: "hidden",
    verticalAlign: "bottom"
  },
  ".cm-widgetBuffer": {
    verticalAlign: "text-top",
    height: "1em",
    width: 0,
    display: "inline"
  },
  ".cm-placeholder": {
    color: "#888",
    display: "inline-block",
    verticalAlign: "top",
    userSelect: "none"
  },
  ".cm-highlightSpace": {
    backgroundImage: "radial-gradient(circle at 50% 55%, #aaa 20%, transparent 5%)",
    backgroundPosition: "center"
  },
  ".cm-highlightTab": {
    backgroundImage: `url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="20"><path stroke="%23888" stroke-width="1" fill="none" d="M1 10H196L190 5M190 15L196 10M197 4L197 16"/></svg>')`,
    backgroundSize: "auto 100%",
    backgroundPosition: "right 90%",
    backgroundRepeat: "no-repeat"
  },
  ".cm-trailingSpace": {
    backgroundColor: "#ff332255"
  },
  ".cm-button": {
    verticalAlign: "middle",
    color: "inherit",
    fontSize: "70%",
    padding: ".2em 1em",
    borderRadius: "1px"
  },
  "&light .cm-button": {
    backgroundImage: "linear-gradient(#eff1f5, #d9d9df)",
    border: "1px solid #888",
    "&:active": {
      backgroundImage: "linear-gradient(#b4b4b4, #d0d3d6)"
    }
  },
  "&dark .cm-button": {
    backgroundImage: "linear-gradient(#393939, #111)",
    border: "1px solid #888",
    "&:active": {
      backgroundImage: "linear-gradient(#111, #333)"
    }
  },
  ".cm-textfield": {
    verticalAlign: "middle",
    color: "inherit",
    fontSize: "70%",
    border: "1px solid silver",
    padding: ".2em .5em"
  },
  "&light .cm-textfield": {
    backgroundColor: "white"
  },
  "&dark .cm-textfield": {
    border: "1px solid #555",
    backgroundColor: "inherit"
  }
}, lightDarkIDs);
var observeOptions = {
  childList: true,
  characterData: true,
  subtree: true,
  attributes: true,
  characterDataOldValue: true
};
var useCharData = browser.ie && browser.ie_version <= 11;
var DOMObserver = class {
  constructor(view) {
    this.view = view;
    this.active = false;
    this.editContext = null;
    this.selectionRange = new DOMSelectionState();
    this.selectionChanged = false;
    this.delayedFlush = -1;
    this.resizeTimeout = -1;
    this.queue = [];
    this.delayedAndroidKey = null;
    this.flushingAndroidKey = -1;
    this.lastChange = 0;
    this.scrollTargets = [];
    this.intersection = null;
    this.resizeScroll = null;
    this.intersecting = false;
    this.gapIntersection = null;
    this.gaps = [];
    this.printQuery = null;
    this.parentCheck = -1;
    this.dom = view.contentDOM;
    this.observer = new MutationObserver((mutations) => {
      for (let mut of mutations)
        this.queue.push(mut);
      if ((browser.ie && browser.ie_version <= 11 || browser.ios && view.composing) && mutations.some((m) => m.type == "childList" && m.removedNodes.length || m.type == "characterData" && m.oldValue.length > m.target.nodeValue.length))
        this.flushSoon();
      else
        this.flush();
    });
    if (window.EditContext && browser.android && view.constructor.EDIT_CONTEXT !== false && // Chrome <126 doesn't support inverted selections in edit context (#1392)
    !(browser.chrome && browser.chrome_version < 126)) {
      this.editContext = new EditContextManager(view);
      if (view.state.facet(editable))
        view.contentDOM.editContext = this.editContext.editContext;
    }
    if (useCharData)
      this.onCharData = (event) => {
        this.queue.push({
          target: event.target,
          type: "characterData",
          oldValue: event.prevValue
        });
        this.flushSoon();
      };
    this.onSelectionChange = this.onSelectionChange.bind(this);
    this.onResize = this.onResize.bind(this);
    this.onPrint = this.onPrint.bind(this);
    this.onScroll = this.onScroll.bind(this);
    if (window.matchMedia)
      this.printQuery = window.matchMedia("print");
    if (typeof ResizeObserver == "function") {
      this.resizeScroll = new ResizeObserver(() => {
        var _a2;
        if (((_a2 = this.view.docView) === null || _a2 === void 0 ? void 0 : _a2.lastUpdate) < Date.now() - 75)
          this.onResize();
      });
      this.resizeScroll.observe(view.scrollDOM);
    }
    this.addWindowListeners(this.win = view.win);
    this.start();
    if (typeof IntersectionObserver == "function") {
      this.intersection = new IntersectionObserver((entries) => {
        if (this.parentCheck < 0)
          this.parentCheck = setTimeout(this.listenForScroll.bind(this), 1e3);
        if (entries.length > 0 && entries[entries.length - 1].intersectionRatio > 0 != this.intersecting) {
          this.intersecting = !this.intersecting;
          if (this.intersecting != this.view.inView)
            this.onScrollChanged(document.createEvent("Event"));
        }
      }, { threshold: [0, 1e-3] });
      this.intersection.observe(this.dom);
      this.gapIntersection = new IntersectionObserver((entries) => {
        if (entries.length > 0 && entries[entries.length - 1].intersectionRatio > 0)
          this.onScrollChanged(document.createEvent("Event"));
      }, {});
    }
    this.listenForScroll();
    this.readSelectionRange();
  }
  onScrollChanged(e) {
    this.view.inputState.runHandlers("scroll", e);
    if (this.intersecting)
      this.view.measure();
  }
  onScroll(e) {
    if (this.intersecting)
      this.flush(false);
    if (this.editContext)
      this.view.requestMeasure(this.editContext.measureReq);
    this.onScrollChanged(e);
  }
  onResize() {
    if (this.resizeTimeout < 0)
      this.resizeTimeout = setTimeout(() => {
        this.resizeTimeout = -1;
        this.view.requestMeasure();
      }, 50);
  }
  onPrint(event) {
    if ((event.type == "change" || !event.type) && !event.matches)
      return;
    this.view.viewState.printing = true;
    this.view.measure();
    setTimeout(() => {
      this.view.viewState.printing = false;
      this.view.requestMeasure();
    }, 500);
  }
  updateGaps(gaps) {
    if (this.gapIntersection && (gaps.length != this.gaps.length || this.gaps.some((g, i) => g != gaps[i]))) {
      this.gapIntersection.disconnect();
      for (let gap of gaps)
        this.gapIntersection.observe(gap);
      this.gaps = gaps;
    }
  }
  onSelectionChange(event) {
    let wasChanged = this.selectionChanged;
    if (!this.readSelectionRange() || this.delayedAndroidKey)
      return;
    let { view } = this, sel = this.selectionRange;
    if (view.state.facet(editable) ? view.root.activeElement != this.dom : !hasSelection(this.dom, sel))
      return;
    let context = sel.anchorNode && view.docView.tile.nearest(sel.anchorNode);
    if (context && context.isWidget() && context.widget.ignoreEvent(event)) {
      if (!wasChanged)
        this.selectionChanged = false;
      return;
    }
    if ((browser.ie && browser.ie_version <= 11 || browser.android && browser.chrome) && !view.state.selection.main.empty && // (Selection.isCollapsed isn't reliable on IE)
    sel.focusNode && isEquivalentPosition(sel.focusNode, sel.focusOffset, sel.anchorNode, sel.anchorOffset))
      this.flushSoon();
    else
      this.flush(false);
  }
  readSelectionRange() {
    let { view } = this;
    let selection = getSelection(view.root);
    if (!selection)
      return false;
    let range = browser.safari && view.root.nodeType == 11 && view.root.activeElement == this.dom && safariSelectionRangeHack(this.view, selection) || selection;
    if (!range || this.selectionRange.eq(range))
      return false;
    let local = hasSelection(this.dom, range);
    if (local && !this.selectionChanged && view.inputState.lastFocusTime > Date.now() - 200 && view.inputState.lastTouchTime < Date.now() - 300 && atElementStart(this.dom, range)) {
      this.view.inputState.lastFocusTime = 0;
      view.docView.updateSelection();
      return false;
    }
    this.selectionRange.setRange(range);
    if (local)
      this.selectionChanged = true;
    return true;
  }
  setSelectionRange(anchor, head) {
    this.selectionRange.set(anchor.node, anchor.offset, head.node, head.offset);
    this.selectionChanged = false;
  }
  clearSelectionRange() {
    this.selectionRange.set(null, 0, null, 0);
  }
  listenForScroll() {
    this.parentCheck = -1;
    let i = 0, changed = null;
    for (let dom = this.dom; dom; ) {
      if (dom.nodeType == 1) {
        if (!changed && i < this.scrollTargets.length && this.scrollTargets[i] == dom)
          i++;
        else if (!changed)
          changed = this.scrollTargets.slice(0, i);
        if (changed)
          changed.push(dom);
        dom = dom.assignedSlot || dom.parentNode;
      } else if (dom.nodeType == 11) {
        dom = dom.host;
      } else {
        break;
      }
    }
    if (i < this.scrollTargets.length && !changed)
      changed = this.scrollTargets.slice(0, i);
    if (changed) {
      for (let dom of this.scrollTargets)
        dom.removeEventListener("scroll", this.onScroll);
      for (let dom of this.scrollTargets = changed)
        dom.addEventListener("scroll", this.onScroll);
    }
  }
  ignore(f) {
    if (!this.active)
      return f();
    try {
      this.stop();
      return f();
    } finally {
      this.start();
      this.clear();
    }
  }
  start() {
    if (this.active)
      return;
    this.observer.observe(this.dom, observeOptions);
    if (useCharData)
      this.dom.addEventListener("DOMCharacterDataModified", this.onCharData);
    this.active = true;
  }
  stop() {
    if (!this.active)
      return;
    this.active = false;
    this.observer.disconnect();
    if (useCharData)
      this.dom.removeEventListener("DOMCharacterDataModified", this.onCharData);
  }
  // Throw away any pending changes
  clear() {
    this.processRecords();
    this.queue.length = 0;
    this.selectionChanged = false;
  }
  // Chrome Android, especially in combination with GBoard, not only
  // doesn't reliably fire regular key events, but also often
  // surrounds the effect of enter or backspace with a bunch of
  // composition events that, when interrupted, cause text duplication
  // or other kinds of corruption. This hack makes the editor back off
  // from handling DOM changes for a moment when such a key is
  // detected (via beforeinput or keydown), and then tries to flush
  // them or, if that has no effect, dispatches the given key.
  delayAndroidKey(key, keyCode) {
    var _a2;
    if (!this.delayedAndroidKey) {
      let flush = () => {
        let key2 = this.delayedAndroidKey;
        if (key2) {
          this.clearDelayedAndroidKey();
          this.view.inputState.lastKeyCode = key2.keyCode;
          this.view.inputState.lastKeyTime = Date.now();
          let flushed = this.flush();
          if (!flushed && key2.force)
            dispatchKey(this.dom, key2.key, key2.keyCode);
        }
      };
      this.flushingAndroidKey = this.view.win.requestAnimationFrame(flush);
    }
    if (!this.delayedAndroidKey || key == "Enter")
      this.delayedAndroidKey = {
        key,
        keyCode,
        // Only run the key handler when no changes are detected if
        // this isn't coming right after another change, in which case
        // it is probably part of a weird chain of updates, and should
        // be ignored if it returns the DOM to its previous state.
        force: this.lastChange < Date.now() - 50 || !!((_a2 = this.delayedAndroidKey) === null || _a2 === void 0 ? void 0 : _a2.force)
      };
  }
  clearDelayedAndroidKey() {
    this.win.cancelAnimationFrame(this.flushingAndroidKey);
    this.delayedAndroidKey = null;
    this.flushingAndroidKey = -1;
  }
  flushSoon() {
    if (this.delayedFlush < 0)
      this.delayedFlush = this.view.win.requestAnimationFrame(() => {
        this.delayedFlush = -1;
        this.flush();
      });
  }
  forceFlush() {
    if (this.delayedFlush >= 0) {
      this.view.win.cancelAnimationFrame(this.delayedFlush);
      this.delayedFlush = -1;
    }
    this.flush();
  }
  pendingRecords() {
    for (let mut of this.observer.takeRecords())
      this.queue.push(mut);
    return this.queue;
  }
  processRecords() {
    let records = this.pendingRecords();
    if (records.length)
      this.queue = [];
    let from = -1, to = -1, typeOver = false;
    for (let record of records) {
      let range = this.readMutation(record);
      if (!range)
        continue;
      if (range.typeOver)
        typeOver = true;
      if (from == -1) {
        ({ from, to } = range);
      } else {
        from = Math.min(range.from, from);
        to = Math.max(range.to, to);
      }
    }
    return { from, to, typeOver };
  }
  readChange() {
    let { from, to, typeOver } = this.processRecords();
    let newSel = this.selectionChanged && hasSelection(this.dom, this.selectionRange);
    if (from < 0 && !newSel)
      return null;
    if (from > -1)
      this.lastChange = Date.now();
    this.view.inputState.lastFocusTime = 0;
    this.selectionChanged = false;
    let change = new DOMChange(this.view, from, to, typeOver);
    this.view.docView.domChanged = { newSel: change.newSel ? change.newSel.main : null };
    return change;
  }
  // Apply pending changes, if any
  flush(readSelection = true) {
    if (this.delayedFlush >= 0 || this.delayedAndroidKey)
      return false;
    if (readSelection)
      this.readSelectionRange();
    let domChange = this.readChange();
    if (!domChange) {
      this.view.requestMeasure();
      return false;
    }
    let startState = this.view.state;
    let handled = applyDOMChange(this.view, domChange);
    if (this.view.state == startState && (domChange.domChanged || domChange.newSel && !domChange.newSel.main.eq(this.view.state.selection.main)))
      this.view.update([]);
    return handled;
  }
  readMutation(rec) {
    let tile = this.view.docView.tile.nearest(rec.target);
    if (!tile || tile.isWidget())
      return null;
    tile.markDirty(rec.type == "attributes");
    if (rec.type == "childList") {
      let childBefore = findChild(tile, rec.previousSibling || rec.target.previousSibling, -1);
      let childAfter = findChild(tile, rec.nextSibling || rec.target.nextSibling, 1);
      return {
        from: childBefore ? tile.posAfter(childBefore) : tile.posAtStart,
        to: childAfter ? tile.posBefore(childAfter) : tile.posAtEnd,
        typeOver: false
      };
    } else if (rec.type == "characterData") {
      return { from: tile.posAtStart, to: tile.posAtEnd, typeOver: rec.target.nodeValue == rec.oldValue };
    } else {
      return null;
    }
  }
  setWindow(win) {
    if (win != this.win) {
      this.removeWindowListeners(this.win);
      this.win = win;
      this.addWindowListeners(this.win);
    }
  }
  addWindowListeners(win) {
    win.addEventListener("resize", this.onResize);
    if (this.printQuery) {
      if (this.printQuery.addEventListener)
        this.printQuery.addEventListener("change", this.onPrint);
      else
        this.printQuery.addListener(this.onPrint);
    } else
      win.addEventListener("beforeprint", this.onPrint);
    win.addEventListener("scroll", this.onScroll);
    win.document.addEventListener("selectionchange", this.onSelectionChange);
  }
  removeWindowListeners(win) {
    win.removeEventListener("scroll", this.onScroll);
    win.removeEventListener("resize", this.onResize);
    if (this.printQuery) {
      if (this.printQuery.removeEventListener)
        this.printQuery.removeEventListener("change", this.onPrint);
      else
        this.printQuery.removeListener(this.onPrint);
    } else
      win.removeEventListener("beforeprint", this.onPrint);
    win.document.removeEventListener("selectionchange", this.onSelectionChange);
  }
  update(update) {
    if (this.editContext) {
      this.editContext.update(update);
      if (update.startState.facet(editable) != update.state.facet(editable))
        update.view.contentDOM.editContext = update.state.facet(editable) ? this.editContext.editContext : null;
    }
  }
  destroy() {
    var _a2, _b, _c;
    this.stop();
    (_a2 = this.intersection) === null || _a2 === void 0 ? void 0 : _a2.disconnect();
    (_b = this.gapIntersection) === null || _b === void 0 ? void 0 : _b.disconnect();
    (_c = this.resizeScroll) === null || _c === void 0 ? void 0 : _c.disconnect();
    for (let dom of this.scrollTargets)
      dom.removeEventListener("scroll", this.onScroll);
    this.removeWindowListeners(this.win);
    clearTimeout(this.parentCheck);
    clearTimeout(this.resizeTimeout);
    this.win.cancelAnimationFrame(this.delayedFlush);
    this.win.cancelAnimationFrame(this.flushingAndroidKey);
    if (this.editContext) {
      this.view.contentDOM.editContext = null;
      this.editContext.destroy();
    }
  }
};
function findChild(tile, dom, dir) {
  while (dom) {
    let curTile = Tile.get(dom);
    if (curTile && curTile.parent == tile)
      return curTile;
    let parent = dom.parentNode;
    dom = parent != tile.dom ? parent : dir > 0 ? dom.nextSibling : dom.previousSibling;
  }
  return null;
}
function buildSelectionRangeFromRange(view, range) {
  let anchorNode = range.startContainer, anchorOffset = range.startOffset;
  let focusNode = range.endContainer, focusOffset = range.endOffset;
  let curAnchor = view.docView.domAtPos(view.state.selection.main.anchor, 1);
  if (isEquivalentPosition(curAnchor.node, curAnchor.offset, focusNode, focusOffset))
    [anchorNode, anchorOffset, focusNode, focusOffset] = [focusNode, focusOffset, anchorNode, anchorOffset];
  return { anchorNode, anchorOffset, focusNode, focusOffset };
}
function safariSelectionRangeHack(view, selection) {
  if (selection.getComposedRanges) {
    let range = selection.getComposedRanges(view.root)[0];
    if (range)
      return buildSelectionRangeFromRange(view, range);
  }
  let found = null;
  function read(event) {
    event.preventDefault();
    event.stopImmediatePropagation();
    found = event.getTargetRanges()[0];
  }
  view.contentDOM.addEventListener("beforeinput", read, true);
  view.dom.ownerDocument.execCommand("indent");
  view.contentDOM.removeEventListener("beforeinput", read, true);
  return found ? buildSelectionRangeFromRange(view, found) : null;
}
var EditContextManager = class {
  constructor(view) {
    this.from = 0;
    this.to = 0;
    this.pendingContextChange = null;
    this.handlers = /* @__PURE__ */ Object.create(null);
    this.composing = null;
    this.resetRange(view.state);
    let context = this.editContext = new window.EditContext({
      text: view.state.doc.sliceString(this.from, this.to),
      selectionStart: this.toContextPos(Math.max(this.from, Math.min(this.to, view.state.selection.main.anchor))),
      selectionEnd: this.toContextPos(view.state.selection.main.head)
    });
    this.handlers.textupdate = (e) => {
      let main = view.state.selection.main, { anchor, head } = main;
      let from = this.toEditorPos(e.updateRangeStart), to = this.toEditorPos(e.updateRangeEnd);
      if (view.inputState.composing >= 0 && !this.composing)
        this.composing = { contextBase: e.updateRangeStart, editorBase: from, drifted: false };
      let deletes = to - from > e.text.length;
      if (from == this.from && anchor < this.from)
        from = anchor;
      else if (to == this.to && anchor > this.to)
        to = anchor;
      let diff = findDiff(view.state.sliceDoc(from, to), e.text, (deletes ? main.from : main.to) - from, deletes ? "end" : null);
      if (!diff) {
        let newSel = EditorSelection.single(this.toEditorPos(e.selectionStart), this.toEditorPos(e.selectionEnd));
        if (!newSel.main.eq(main))
          view.dispatch({ selection: newSel, userEvent: "select" });
        return;
      }
      let change = {
        from: diff.from + from,
        to: diff.toA + from,
        insert: Text.of(e.text.slice(diff.from, diff.toB).split("\n"))
      };
      if ((browser.mac || browser.android) && change.from == head - 1 && /^\. ?$/.test(e.text) && view.contentDOM.getAttribute("autocorrect") == "off")
        change = { from, to, insert: Text.of([e.text.replace(".", " ")]) };
      this.pendingContextChange = change;
      if (!view.state.readOnly) {
        let newLen = this.to - this.from + (change.to - change.from + change.insert.length);
        applyDOMChangeInner(view, change, EditorSelection.single(this.toEditorPos(e.selectionStart, newLen), this.toEditorPos(e.selectionEnd, newLen)));
      }
      if (this.pendingContextChange) {
        this.revertPending(view.state);
        this.setSelection(view.state);
      }
      if (change.from < change.to && !change.insert.length && view.inputState.composing >= 0 && !/[\\p{Alphabetic}\\p{Number}_]/.test(context.text.slice(Math.max(0, e.updateRangeStart - 1), Math.min(context.text.length, e.updateRangeStart + 1))))
        this.handlers.compositionend(e);
    };
    this.handlers.characterboundsupdate = (e) => {
      let rects = [], prev = null;
      for (let i = this.toEditorPos(e.rangeStart), end = this.toEditorPos(e.rangeEnd); i < end; i++) {
        let rect = view.coordsForChar(i);
        prev = rect && new DOMRect(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top) || prev || new DOMRect();
        rects.push(prev);
      }
      context.updateCharacterBounds(e.rangeStart, rects);
    };
    this.handlers.textformatupdate = (e) => {
      let deco = [];
      for (let format of e.getTextFormats()) {
        let lineStyle = format.underlineStyle, thickness = format.underlineThickness;
        if (!/none/i.test(lineStyle) && !/none/i.test(thickness)) {
          let from = this.toEditorPos(format.rangeStart), to = this.toEditorPos(format.rangeEnd);
          if (from < to) {
            let style = `text-decoration: underline ${/^[a-z]/.test(lineStyle) ? lineStyle + " " : lineStyle == "Dashed" ? "dashed " : lineStyle == "Squiggle" ? "wavy " : ""}${/thin/i.test(thickness) ? 1 : 2}px`;
            deco.push(Decoration.mark({ attributes: { style } }).range(from, to));
          }
        }
      }
      view.dispatch({ effects: setEditContextFormatting.of(Decoration.set(deco)) });
    };
    this.handlers.compositionstart = () => {
      if (view.inputState.composing < 0) {
        view.inputState.composing = 0;
        view.inputState.compositionFirstChange = true;
      }
    };
    this.handlers.compositionend = () => {
      view.inputState.composing = -1;
      view.inputState.compositionFirstChange = null;
      if (this.composing) {
        let { drifted } = this.composing;
        this.composing = null;
        if (drifted)
          this.reset(view.state);
      }
    };
    for (let event in this.handlers)
      context.addEventListener(event, this.handlers[event]);
    this.measureReq = { read: (view2) => {
      this.editContext.updateControlBounds(view2.contentDOM.getBoundingClientRect());
      let sel = getSelection(view2.root);
      if (sel && sel.rangeCount)
        this.editContext.updateSelectionBounds(sel.getRangeAt(0).getBoundingClientRect());
    } };
  }
  applyEdits(update) {
    let off = 0, abort = false, pending = this.pendingContextChange;
    update.changes.iterChanges((fromA, toA, _fromB, _toB, insert2) => {
      if (abort)
        return;
      let dLen = insert2.length - (toA - fromA);
      if (pending && toA >= pending.to) {
        if (pending.from == fromA && pending.to == toA && pending.insert.eq(insert2)) {
          pending = this.pendingContextChange = null;
          off += dLen;
          this.to += dLen;
          return;
        } else {
          pending = null;
          this.revertPending(update.state);
        }
      }
      fromA += off;
      toA += off;
      if (toA <= this.from) {
        this.from += dLen;
        this.to += dLen;
      } else if (fromA < this.to) {
        if (fromA < this.from || toA > this.to || this.to - this.from + insert2.length > 3e4) {
          abort = true;
          return;
        }
        this.editContext.updateText(this.toContextPos(fromA), this.toContextPos(toA), insert2.toString());
        this.to += dLen;
      }
      off += dLen;
    });
    if (pending && !abort)
      this.revertPending(update.state);
    return !abort;
  }
  update(update) {
    let reverted = this.pendingContextChange, startSel = update.startState.selection.main;
    if (this.composing && (this.composing.drifted || !update.changes.touchesRange(startSel.from, startSel.to) && update.transactions.some((tr) => !tr.isUserEvent("input.type") && tr.changes.touchesRange(this.from, this.to)))) {
      this.composing.drifted = true;
      this.composing.editorBase = update.changes.mapPos(this.composing.editorBase);
    } else if (!this.applyEdits(update) || !this.rangeIsValid(update.state)) {
      this.pendingContextChange = null;
      this.reset(update.state);
    } else if (update.docChanged || update.selectionSet || reverted) {
      this.setSelection(update.state);
    }
    if (update.geometryChanged || update.docChanged || update.selectionSet)
      update.view.requestMeasure(this.measureReq);
  }
  resetRange(state) {
    let { head } = state.selection.main;
    this.from = Math.max(
      0,
      head - 1e4
      /* CxVp.Margin */
    );
    this.to = Math.min(
      state.doc.length,
      head + 1e4
      /* CxVp.Margin */
    );
  }
  reset(state) {
    this.resetRange(state);
    this.editContext.updateText(0, this.editContext.text.length, state.doc.sliceString(this.from, this.to));
    this.setSelection(state);
  }
  revertPending(state) {
    let pending = this.pendingContextChange;
    this.pendingContextChange = null;
    this.editContext.updateText(this.toContextPos(pending.from), this.toContextPos(pending.from + pending.insert.length), state.doc.sliceString(pending.from, pending.to));
  }
  setSelection(state) {
    let { main } = state.selection;
    let start = this.toContextPos(Math.max(this.from, Math.min(this.to, main.anchor)));
    let end = this.toContextPos(main.head);
    if (this.editContext.selectionStart != start || this.editContext.selectionEnd != end)
      this.editContext.updateSelection(start, end);
  }
  rangeIsValid(state) {
    let { head } = state.selection.main;
    return !(this.from > 0 && head - this.from < 500 || this.to < state.doc.length && this.to - head < 500 || this.to - this.from > 1e4 * 3);
  }
  toEditorPos(contextPos, clipLen = this.to - this.from) {
    contextPos = Math.min(contextPos, clipLen);
    let c = this.composing;
    return c && c.drifted ? c.editorBase + (contextPos - c.contextBase) : contextPos + this.from;
  }
  toContextPos(editorPos) {
    let c = this.composing;
    return c && c.drifted ? c.contextBase + (editorPos - c.editorBase) : editorPos - this.from;
  }
  destroy() {
    for (let event in this.handlers)
      this.editContext.removeEventListener(event, this.handlers[event]);
  }
};
var EditorView = class _EditorView {
  /**
  The current editor state.
  */
  get state() {
    return this.viewState.state;
  }
  /**
  To be able to display large documents without consuming too much
  memory or overloading the browser, CodeMirror only draws the
  code that is visible (plus a margin around it) to the DOM. This
  property tells you the extent of the current drawn viewport, in
  document positions.
  */
  get viewport() {
    return this.viewState.viewport;
  }
  /**
  When there are, for example, large collapsed ranges in the
  viewport, its size can be a lot bigger than the actual visible
  content. Thus, if you are doing something like styling the
  content in the viewport, it is preferable to only do so for
  these ranges, which are the subset of the viewport that is
  actually drawn.
  */
  get visibleRanges() {
    return this.viewState.visibleRanges;
  }
  /**
  Returns false when the editor is entirely scrolled out of view
  or otherwise hidden.
  */
  get inView() {
    return this.viewState.inView;
  }
  /**
  Indicates whether the user is currently composing text via
  [IME](https://en.wikipedia.org/wiki/Input_method), and at least
  one change has been made in the current composition.
  */
  get composing() {
    return !!this.inputState && this.inputState.composing > 0;
  }
  /**
  Indicates whether the user is currently in composing state. Note
  that on some platforms, like Android, this will be the case a
  lot, since just putting the cursor on a word starts a
  composition there.
  */
  get compositionStarted() {
    return !!this.inputState && this.inputState.composing >= 0;
  }
  /**
  The document or shadow root that the view lives in.
  */
  get root() {
    return this._root;
  }
  /**
  @internal
  */
  get win() {
    return this.dom.ownerDocument.defaultView || window;
  }
  /**
  Construct a new view. You'll want to either provide a `parent`
  option, or put `view.dom` into your document after creating a
  view, so that the user can see the editor.
  */
  constructor(config = {}) {
    var _a2;
    this.plugins = [];
    this.pluginMap = /* @__PURE__ */ new Map();
    this.editorAttrs = {};
    this.contentAttrs = {};
    this.bidiCache = [];
    this.destroyed = false;
    this.updateState = 2;
    this.measureScheduled = -1;
    this.measureRequests = [];
    this.contentDOM = document.createElement("div");
    this.scrollDOM = document.createElement("div");
    this.scrollDOM.tabIndex = -1;
    this.scrollDOM.className = "cm-scroller";
    this.scrollDOM.appendChild(this.contentDOM);
    this.announceDOM = document.createElement("div");
    this.announceDOM.className = "cm-announced";
    this.announceDOM.setAttribute("aria-live", "polite");
    this.dom = document.createElement("div");
    this.dom.appendChild(this.announceDOM);
    this.dom.appendChild(this.scrollDOM);
    if (config.parent)
      config.parent.appendChild(this.dom);
    let { dispatch } = config;
    this.dispatchTransactions = config.dispatchTransactions || dispatch && ((trs) => trs.forEach((tr) => dispatch(tr, this))) || ((trs) => this.update(trs));
    this.dispatch = this.dispatch.bind(this);
    this._root = config.root || getRoot(config.parent) || document;
    this.viewState = new ViewState(config.state || EditorState.create(config));
    if (config.scrollTo && config.scrollTo.is(scrollIntoView))
      this.viewState.scrollTarget = config.scrollTo.value.clip(this.viewState.state);
    this.plugins = this.state.facet(viewPlugin).map((spec) => new PluginInstance(spec));
    for (let plugin of this.plugins)
      plugin.update(this);
    this.observer = new DOMObserver(this);
    this.inputState = new InputState(this);
    this.inputState.ensureHandlers(this.plugins);
    this.docView = new DocView(this);
    this.mountStyles();
    this.updateAttrs();
    this.updateState = 0;
    this.requestMeasure();
    if ((_a2 = document.fonts) === null || _a2 === void 0 ? void 0 : _a2.ready)
      document.fonts.ready.then(() => this.requestMeasure());
  }
  dispatch(...input) {
    let trs = input.length == 1 && input[0] instanceof Transaction ? input : input.length == 1 && Array.isArray(input[0]) ? input[0] : [this.state.update(...input)];
    this.dispatchTransactions(trs, this);
  }
  /**
  Update the view for the given array of transactions. This will
  update the visible document and selection to match the state
  produced by the transactions, and notify view plugins of the
  change. You should usually call
  [`dispatch`](https://codemirror.net/6/docs/ref/#view.EditorView.dispatch) instead, which uses this
  as a primitive.
  */
  update(transactions) {
    if (this.updateState != 0)
      throw new Error("Calls to EditorView.update are not allowed while an update is in progress");
    let redrawn = false, attrsChanged = false, update;
    let state = this.state;
    for (let tr of transactions) {
      if (tr.startState != state)
        throw new RangeError("Trying to update state with a transaction that doesn't start from the previous state.");
      state = tr.state;
    }
    if (this.destroyed) {
      this.viewState.state = state;
      return;
    }
    let focus = this.hasFocus, focusFlag = 0, dispatchFocus = null;
    if (transactions.some((tr) => tr.annotation(isFocusChange))) {
      this.inputState.notifiedFocused = focus;
      focusFlag = 1;
    } else if (focus != this.inputState.notifiedFocused) {
      this.inputState.notifiedFocused = focus;
      dispatchFocus = focusChangeTransaction(state, focus);
      if (!dispatchFocus)
        focusFlag = 1;
    }
    let pendingKey = this.observer.delayedAndroidKey, domChange = null;
    if (pendingKey) {
      this.observer.clearDelayedAndroidKey();
      domChange = this.observer.readChange();
      if (domChange && !this.state.doc.eq(state.doc) || !this.state.selection.eq(state.selection))
        domChange = null;
    } else {
      this.observer.clear();
    }
    if (state.facet(EditorState.phrases) != this.state.facet(EditorState.phrases))
      return this.setState(state);
    update = ViewUpdate.create(this, state, transactions);
    update.flags |= focusFlag;
    let scrollTarget = this.viewState.scrollTarget;
    try {
      this.updateState = 2;
      for (let tr of transactions) {
        if (scrollTarget)
          scrollTarget = scrollTarget.map(tr.changes);
        if (tr.scrollIntoView) {
          let { main } = tr.state.selection;
          scrollTarget = new ScrollTarget(main.empty ? main : EditorSelection.cursor(main.head, main.head > main.anchor ? -1 : 1));
        }
        for (let e of tr.effects)
          if (e.is(scrollIntoView))
            scrollTarget = e.value.clip(this.state);
      }
      this.viewState.update(update, scrollTarget);
      this.bidiCache = CachedOrder.update(this.bidiCache, update.changes);
      if (!update.empty) {
        this.updatePlugins(update);
        this.inputState.update(update);
      }
      redrawn = this.docView.update(update);
      if (this.state.facet(styleModule) != this.styleModules)
        this.mountStyles();
      attrsChanged = this.updateAttrs();
      this.showAnnouncements(transactions);
      this.docView.updateSelection(redrawn, transactions.some((tr) => tr.isUserEvent("select.pointer")));
    } finally {
      this.updateState = 0;
    }
    if (update.startState.facet(theme) != update.state.facet(theme))
      this.viewState.mustMeasureContent = true;
    if (redrawn || attrsChanged || scrollTarget || this.viewState.mustEnforceCursorAssoc || this.viewState.mustMeasureContent)
      this.requestMeasure();
    if (redrawn)
      this.docViewUpdate();
    if (!update.empty)
      for (let listener of this.state.facet(updateListener)) {
        try {
          listener(update);
        } catch (e) {
          logException(this.state, e, "update listener");
        }
      }
    if (dispatchFocus || domChange)
      Promise.resolve().then(() => {
        if (dispatchFocus && this.state == dispatchFocus.startState)
          this.dispatch(dispatchFocus);
        if (domChange) {
          if (!applyDOMChange(this, domChange) && pendingKey.force)
            dispatchKey(this.contentDOM, pendingKey.key, pendingKey.keyCode);
        }
      });
  }
  /**
  Reset the view to the given state. (This will cause the entire
  document to be redrawn and all view plugins to be reinitialized,
  so you should probably only use it when the new state isn't
  derived from the old state. Otherwise, use
  [`dispatch`](https://codemirror.net/6/docs/ref/#view.EditorView.dispatch) instead.)
  */
  setState(newState) {
    if (this.updateState != 0)
      throw new Error("Calls to EditorView.setState are not allowed while an update is in progress");
    if (this.destroyed) {
      this.viewState.state = newState;
      return;
    }
    this.updateState = 2;
    let hadFocus = this.hasFocus;
    try {
      for (let plugin of this.plugins)
        plugin.destroy(this);
      this.viewState = new ViewState(newState);
      this.plugins = newState.facet(viewPlugin).map((spec) => new PluginInstance(spec));
      this.pluginMap.clear();
      for (let plugin of this.plugins)
        plugin.update(this);
      this.docView.destroy();
      this.docView = new DocView(this);
      this.inputState.ensureHandlers(this.plugins);
      this.mountStyles();
      this.updateAttrs();
      this.bidiCache = [];
    } finally {
      this.updateState = 0;
    }
    if (hadFocus)
      this.focus();
    this.requestMeasure();
  }
  updatePlugins(update) {
    let prevSpecs = update.startState.facet(viewPlugin), specs = update.state.facet(viewPlugin);
    if (prevSpecs != specs) {
      let newPlugins = [];
      for (let spec of specs) {
        let found = prevSpecs.indexOf(spec);
        if (found < 0) {
          newPlugins.push(new PluginInstance(spec));
        } else {
          let plugin = this.plugins[found];
          plugin.mustUpdate = update;
          newPlugins.push(plugin);
        }
      }
      for (let plugin of this.plugins)
        if (plugin.mustUpdate != update)
          plugin.destroy(this);
      this.plugins = newPlugins;
      this.pluginMap.clear();
    } else {
      for (let p of this.plugins)
        p.mustUpdate = update;
    }
    for (let i = 0; i < this.plugins.length; i++)
      this.plugins[i].update(this);
    if (prevSpecs != specs)
      this.inputState.ensureHandlers(this.plugins);
  }
  docViewUpdate() {
    for (let plugin of this.plugins) {
      let val = plugin.value;
      if (val && val.docViewUpdate) {
        try {
          val.docViewUpdate(this);
        } catch (e) {
          logException(this.state, e, "doc view update listener");
        }
      }
    }
  }
  /**
  @internal
  */
  measure(flush = true) {
    if (this.destroyed)
      return;
    if (this.measureScheduled > -1)
      this.win.cancelAnimationFrame(this.measureScheduled);
    if (this.observer.delayedAndroidKey) {
      this.measureScheduled = -1;
      this.requestMeasure();
      return;
    }
    this.measureScheduled = 0;
    if (flush)
      this.observer.forceFlush();
    let updated = null;
    let sDOM = this.scrollDOM, scrollTop = sDOM.scrollTop * this.scaleY;
    let { scrollAnchorPos, scrollAnchorHeight } = this.viewState;
    if (Math.abs(scrollTop - this.viewState.scrollTop) > 1)
      scrollAnchorHeight = -1;
    this.viewState.scrollAnchorHeight = -1;
    try {
      for (let i = 0; ; i++) {
        if (scrollAnchorHeight < 0) {
          if (isScrolledToBottom(sDOM)) {
            scrollAnchorPos = -1;
            scrollAnchorHeight = this.viewState.heightMap.height;
          } else {
            let block = this.viewState.scrollAnchorAt(scrollTop);
            scrollAnchorPos = block.from;
            scrollAnchorHeight = block.top;
          }
        }
        this.updateState = 1;
        let changed = this.viewState.measure(this);
        if (!changed && !this.measureRequests.length && this.viewState.scrollTarget == null)
          break;
        if (i > 5) {
          console.warn(this.measureRequests.length ? "Measure loop restarted more than 5 times" : "Viewport failed to stabilize");
          break;
        }
        let measuring = [];
        if (!(changed & 4))
          [this.measureRequests, measuring] = [measuring, this.measureRequests];
        let measured = measuring.map((m) => {
          try {
            return m.read(this);
          } catch (e) {
            logException(this.state, e);
            return BadMeasure;
          }
        });
        let update = ViewUpdate.create(this, this.state, []), redrawn = false;
        update.flags |= changed;
        if (!updated)
          updated = update;
        else
          updated.flags |= changed;
        this.updateState = 2;
        if (!update.empty) {
          this.updatePlugins(update);
          this.inputState.update(update);
          this.updateAttrs();
          redrawn = this.docView.update(update);
          if (redrawn)
            this.docViewUpdate();
        }
        for (let i2 = 0; i2 < measuring.length; i2++)
          if (measured[i2] != BadMeasure) {
            try {
              let m = measuring[i2];
              if (m.write)
                m.write(measured[i2], this);
            } catch (e) {
              logException(this.state, e);
            }
          }
        if (redrawn)
          this.docView.updateSelection(true);
        if (!update.viewportChanged && this.measureRequests.length == 0) {
          if (this.viewState.editorHeight) {
            if (this.viewState.scrollTarget) {
              this.docView.scrollIntoView(this.viewState.scrollTarget);
              this.viewState.scrollTarget = null;
              scrollAnchorHeight = -1;
              continue;
            } else {
              let newAnchorHeight = scrollAnchorPos < 0 ? this.viewState.heightMap.height : this.viewState.lineBlockAt(scrollAnchorPos).top;
              let diff = newAnchorHeight - scrollAnchorHeight;
              if (diff > 1 || diff < -1) {
                scrollTop = scrollTop + diff;
                sDOM.scrollTop = scrollTop / this.scaleY;
                scrollAnchorHeight = -1;
                continue;
              }
            }
          }
          break;
        }
      }
    } finally {
      this.updateState = 0;
      this.measureScheduled = -1;
    }
    if (updated && !updated.empty)
      for (let listener of this.state.facet(updateListener))
        listener(updated);
  }
  /**
  Get the CSS classes for the currently active editor themes.
  */
  get themeClasses() {
    return baseThemeID + " " + (this.state.facet(darkTheme) ? baseDarkID : baseLightID) + " " + this.state.facet(theme);
  }
  updateAttrs() {
    let editorAttrs = attrsFromFacet(this, editorAttributes, {
      class: "cm-editor" + (this.hasFocus ? " cm-focused " : " ") + this.themeClasses
    });
    let contentAttrs = {
      spellcheck: "false",
      autocorrect: "off",
      autocapitalize: "off",
      writingsuggestions: "false",
      translate: "no",
      contenteditable: !this.state.facet(editable) ? "false" : "true",
      class: "cm-content",
      style: `${browser.tabSize}: ${this.state.tabSize}`,
      role: "textbox",
      "aria-multiline": "true"
    };
    if (this.state.readOnly)
      contentAttrs["aria-readonly"] = "true";
    attrsFromFacet(this, contentAttributes, contentAttrs);
    let changed = this.observer.ignore(() => {
      let changedContent = updateAttrs(this.contentDOM, this.contentAttrs, contentAttrs);
      let changedEditor = updateAttrs(this.dom, this.editorAttrs, editorAttrs);
      return changedContent || changedEditor;
    });
    this.editorAttrs = editorAttrs;
    this.contentAttrs = contentAttrs;
    return changed;
  }
  showAnnouncements(trs) {
    let first = true;
    for (let tr of trs)
      for (let effect of tr.effects)
        if (effect.is(_EditorView.announce)) {
          if (first)
            this.announceDOM.textContent = "";
          first = false;
          let div = this.announceDOM.appendChild(document.createElement("div"));
          div.textContent = effect.value;
        }
  }
  mountStyles() {
    this.styleModules = this.state.facet(styleModule);
    let nonce = this.state.facet(_EditorView.cspNonce);
    StyleModule.mount(this.root, this.styleModules.concat(baseTheme$1).reverse(), nonce ? { nonce } : void 0);
  }
  readMeasured() {
    if (this.updateState == 2)
      throw new Error("Reading the editor layout isn't allowed during an update");
    if (this.updateState == 0 && this.measureScheduled > -1)
      this.measure(false);
  }
  /**
  Schedule a layout measurement, optionally providing callbacks to
  do custom DOM measuring followed by a DOM write phase. Using
  this is preferable reading DOM layout directly from, for
  example, an event handler, because it'll make sure measuring and
  drawing done by other components is synchronized, avoiding
  unnecessary DOM layout computations.
  */
  requestMeasure(request) {
    if (this.measureScheduled < 0)
      this.measureScheduled = this.win.requestAnimationFrame(() => this.measure());
    if (request) {
      if (this.measureRequests.indexOf(request) > -1)
        return;
      if (request.key != null)
        for (let i = 0; i < this.measureRequests.length; i++) {
          if (this.measureRequests[i].key === request.key) {
            this.measureRequests[i] = request;
            return;
          }
        }
      this.measureRequests.push(request);
    }
  }
  /**
  Get the value of a specific plugin, if present. Note that
  plugins that crash can be dropped from a view, so even when you
  know you registered a given plugin, it is recommended to check
  the return value of this method.
  */
  plugin(plugin) {
    let known = this.pluginMap.get(plugin);
    if (known === void 0 || known && known.plugin != plugin)
      this.pluginMap.set(plugin, known = this.plugins.find((p) => p.plugin == plugin) || null);
    return known && known.update(this).value;
  }
  /**
  The top position of the document, in screen coordinates. This
  may be negative when the editor is scrolled down. Points
  directly to the top of the first line, not above the padding.
  */
  get documentTop() {
    return this.contentDOM.getBoundingClientRect().top + this.viewState.paddingTop;
  }
  /**
  Reports the padding above and below the document.
  */
  get documentPadding() {
    return { top: this.viewState.paddingTop, bottom: this.viewState.paddingBottom };
  }
  /**
  If the editor is transformed with CSS, this provides the scale
  along the X axis. Otherwise, it will just be 1. Note that
  transforms other than translation and scaling are not supported.
  */
  get scaleX() {
    return this.viewState.scaleX;
  }
  /**
  Provide the CSS transformed scale along the Y axis.
  */
  get scaleY() {
    return this.viewState.scaleY;
  }
  /**
  Find the text line or block widget at the given vertical
  position (which is interpreted as relative to the [top of the
  document](https://codemirror.net/6/docs/ref/#view.EditorView.documentTop)).
  */
  elementAtHeight(height) {
    this.readMeasured();
    return this.viewState.elementAtHeight(height);
  }
  /**
  Find the line block (see
  [`lineBlockAt`](https://codemirror.net/6/docs/ref/#view.EditorView.lineBlockAt)) at the given
  height, again interpreted relative to the [top of the
  document](https://codemirror.net/6/docs/ref/#view.EditorView.documentTop).
  */
  lineBlockAtHeight(height) {
    this.readMeasured();
    return this.viewState.lineBlockAtHeight(height);
  }
  /**
  Get the extent and vertical position of all [line
  blocks](https://codemirror.net/6/docs/ref/#view.EditorView.lineBlockAt) in the viewport. Positions
  are relative to the [top of the
  document](https://codemirror.net/6/docs/ref/#view.EditorView.documentTop);
  */
  get viewportLineBlocks() {
    return this.viewState.viewportLines;
  }
  /**
  Find the line block around the given document position. A line
  block is a range delimited on both sides by either a
  non-[hidden](https://codemirror.net/6/docs/ref/#view.Decoration^replace) line break, or the
  start/end of the document. It will usually just hold a line of
  text, but may be broken into multiple textblocks by block
  widgets.
  */
  lineBlockAt(pos) {
    return this.viewState.lineBlockAt(pos);
  }
  /**
  The editor's total content height.
  */
  get contentHeight() {
    return this.viewState.contentHeight;
  }
  /**
  Move a cursor position by [grapheme
  cluster](https://codemirror.net/6/docs/ref/#state.findClusterBreak). `forward` determines whether
  the motion is away from the line start, or towards it. In
  bidirectional text, the line is traversed in visual order, using
  the editor's [text direction](https://codemirror.net/6/docs/ref/#view.EditorView.textDirection).
  When the start position was the last one on the line, the
  returned position will be across the line break. If there is no
  further line, the original position is returned.
  
  By default, this method moves over a single cluster. The
  optional `by` argument can be used to move across more. It will
  be called with the first cluster as argument, and should return
  a predicate that determines, for each subsequent cluster,
  whether it should also be moved over.
  */
  moveByChar(start, forward, by) {
    return skipAtoms(this, start, moveByChar(this, start, forward, by));
  }
  /**
  Move a cursor position across the next group of either
  [letters](https://codemirror.net/6/docs/ref/#state.EditorState.charCategorizer) or non-letter
  non-whitespace characters.
  */
  moveByGroup(start, forward) {
    return skipAtoms(this, start, moveByChar(this, start, forward, (initial) => byGroup(this, start.head, initial)));
  }
  /**
  Get the cursor position visually at the start or end of a line.
  Note that this may differ from the _logical_ position at its
  start or end (which is simply at `line.from`/`line.to`) if text
  at the start or end goes against the line's base text direction.
  */
  visualLineSide(line, end) {
    let order = this.bidiSpans(line), dir = this.textDirectionAt(line.from);
    let span = order[end ? order.length - 1 : 0];
    return EditorSelection.cursor(span.side(end, dir) + line.from, span.forward(!end, dir) ? 1 : -1);
  }
  /**
  Move to the next line boundary in the given direction. If
  `includeWrap` is true, line wrapping is on, and there is a
  further wrap point on the current line, the wrap point will be
  returned. Otherwise this function will return the start or end
  of the line.
  */
  moveToLineBoundary(start, forward, includeWrap = true) {
    return moveToLineBoundary(this, start, forward, includeWrap);
  }
  /**
  Move a cursor position vertically. When `distance` isn't given,
  it defaults to moving to the next line (including wrapped
  lines). Otherwise, `distance` should provide a positive distance
  in pixels.
  
  When `start` has a
  [`goalColumn`](https://codemirror.net/6/docs/ref/#state.SelectionRange.goalColumn), the vertical
  motion will use that as a target horizontal position. Otherwise,
  the cursor's own horizontal position is used. The returned
  cursor will have its goal column set to whichever column was
  used.
  */
  moveVertically(start, forward, distance) {
    return skipAtoms(this, start, moveVertically(this, start, forward, distance));
  }
  /**
  Find the DOM parent node and offset (child offset if `node` is
  an element, character offset when it is a text node) at the
  given document position.
  
  Note that for positions that aren't currently in
  `visibleRanges`, the resulting DOM position isn't necessarily
  meaningful (it may just point before or after a placeholder
  element).
  */
  domAtPos(pos, side = 1) {
    return this.docView.domAtPos(pos, side);
  }
  /**
  Find the document position at the given DOM node. Can be useful
  for associating positions with DOM events. Will raise an error
  when `node` isn't part of the editor content.
  */
  posAtDOM(node, offset = 0) {
    return this.docView.posFromDOM(node, offset);
  }
  posAtCoords(coords, precise = true) {
    this.readMeasured();
    let found = posAtCoords(this, coords, precise);
    return found && found.pos;
  }
  posAndSideAtCoords(coords, precise = true) {
    this.readMeasured();
    return posAtCoords(this, coords, precise);
  }
  /**
  Get the screen coordinates at the given document position.
  `side` determines whether the coordinates are based on the
  element before (-1) or after (1) the position (if no element is
  available on the given side, the method will transparently use
  another strategy to get reasonable coordinates).
  */
  coordsAtPos(pos, side = 1) {
    this.readMeasured();
    let rect = this.docView.coordsAt(pos, side);
    if (!rect || rect.left == rect.right)
      return rect;
    let line = this.state.doc.lineAt(pos), order = this.bidiSpans(line);
    let span = order[BidiSpan.find(order, pos - line.from, -1, side)];
    return flattenRect(rect, span.dir == Direction.LTR == side > 0);
  }
  /**
  Return the rectangle around a given character. If `pos` does not
  point in front of a character that is in the viewport and
  rendered (i.e. not replaced, not a line break), this will return
  null. For space characters that are a line wrap point, this will
  return the position before the line break.
  */
  coordsForChar(pos) {
    this.readMeasured();
    return this.docView.coordsForChar(pos);
  }
  /**
  The default width of a character in the editor. May not
  accurately reflect the width of all characters (given variable
  width fonts or styling of invididual ranges).
  */
  get defaultCharacterWidth() {
    return this.viewState.heightOracle.charWidth;
  }
  /**
  The default height of a line in the editor. May not be accurate
  for all lines.
  */
  get defaultLineHeight() {
    return this.viewState.heightOracle.lineHeight;
  }
  /**
  The text direction
  ([`direction`](https://developer.mozilla.org/en-US/docs/Web/CSS/direction)
  CSS property) of the editor's content element.
  */
  get textDirection() {
    return this.viewState.defaultTextDirection;
  }
  /**
  Find the text direction of the block at the given position, as
  assigned by CSS. If
  [`perLineTextDirection`](https://codemirror.net/6/docs/ref/#view.EditorView^perLineTextDirection)
  isn't enabled, or the given position is outside of the viewport,
  this will always return the same as
  [`textDirection`](https://codemirror.net/6/docs/ref/#view.EditorView.textDirection). Note that
  this may trigger a DOM layout.
  */
  textDirectionAt(pos) {
    let perLine = this.state.facet(perLineTextDirection);
    if (!perLine || pos < this.viewport.from || pos > this.viewport.to)
      return this.textDirection;
    this.readMeasured();
    return this.docView.textDirectionAt(pos);
  }
  /**
  Whether this editor [wraps lines](https://codemirror.net/6/docs/ref/#view.EditorView.lineWrapping)
  (as determined by the
  [`white-space`](https://developer.mozilla.org/en-US/docs/Web/CSS/white-space)
  CSS property of its content element).
  */
  get lineWrapping() {
    return this.viewState.heightOracle.lineWrapping;
  }
  /**
  Returns the bidirectional text structure of the given line
  (which should be in the current document) as an array of span
  objects. The order of these spans matches the [text
  direction](https://codemirror.net/6/docs/ref/#view.EditorView.textDirection)—if that is
  left-to-right, the leftmost spans come first, otherwise the
  rightmost spans come first.
  */
  bidiSpans(line) {
    if (line.length > MaxBidiLine)
      return trivialOrder(line.length);
    let dir = this.textDirectionAt(line.from), isolates;
    for (let entry of this.bidiCache) {
      if (entry.from == line.from && entry.dir == dir && (entry.fresh || isolatesEq(entry.isolates, isolates = getIsolatedRanges(this, line))))
        return entry.order;
    }
    if (!isolates)
      isolates = getIsolatedRanges(this, line);
    let order = computeOrder(line.text, dir, isolates);
    this.bidiCache.push(new CachedOrder(line.from, line.to, dir, isolates, true, order));
    return order;
  }
  /**
  Check whether the editor has focus.
  */
  get hasFocus() {
    var _a2;
    return (this.dom.ownerDocument.hasFocus() || browser.safari && ((_a2 = this.inputState) === null || _a2 === void 0 ? void 0 : _a2.lastContextMenu) > Date.now() - 3e4) && this.root.activeElement == this.contentDOM;
  }
  /**
  Put focus on the editor.
  */
  focus() {
    this.observer.ignore(() => {
      focusPreventScroll(this.contentDOM);
      this.docView.updateSelection();
    });
  }
  /**
  Update the [root](https://codemirror.net/6/docs/ref/##view.EditorViewConfig.root) in which the editor lives. This is only
  necessary when moving the editor's existing DOM to a new window or shadow root.
  */
  setRoot(root) {
    if (this._root != root) {
      this._root = root;
      this.observer.setWindow((root.nodeType == 9 ? root : root.ownerDocument).defaultView || window);
      this.mountStyles();
    }
  }
  /**
  Clean up this editor view, removing its element from the
  document, unregistering event handlers, and notifying
  plugins. The view instance can no longer be used after
  calling this.
  */
  destroy() {
    if (this.root.activeElement == this.contentDOM)
      this.contentDOM.blur();
    for (let plugin of this.plugins)
      plugin.destroy(this);
    this.plugins = [];
    this.inputState.destroy();
    this.docView.destroy();
    this.dom.remove();
    this.observer.destroy();
    if (this.measureScheduled > -1)
      this.win.cancelAnimationFrame(this.measureScheduled);
    this.destroyed = true;
  }
  /**
  Returns an effect that can be
  [added](https://codemirror.net/6/docs/ref/#state.TransactionSpec.effects) to a transaction to
  cause it to scroll the given position or range into view.
  */
  static scrollIntoView(pos, options = {}) {
    return scrollIntoView.of(new ScrollTarget(typeof pos == "number" ? EditorSelection.cursor(pos) : pos, options.y, options.x, options.yMargin, options.xMargin));
  }
  /**
  Return an effect that resets the editor to its current (at the
  time this method was called) scroll position. Note that this
  only affects the editor's own scrollable element, not parents.
  See also
  [`EditorViewConfig.scrollTo`](https://codemirror.net/6/docs/ref/#view.EditorViewConfig.scrollTo).
  
  The effect should be used with a document identical to the one
  it was created for. Failing to do so is not an error, but may
  not scroll to the expected position. You can
  [map](https://codemirror.net/6/docs/ref/#state.StateEffect.map) the effect to account for changes.
  */
  scrollSnapshot() {
    let { scrollTop, scrollLeft } = this.scrollDOM;
    let ref = this.viewState.scrollAnchorAt(scrollTop);
    return scrollIntoView.of(new ScrollTarget(EditorSelection.cursor(ref.from), "start", "start", ref.top - scrollTop, scrollLeft, true));
  }
  /**
  Enable or disable tab-focus mode, which disables key bindings
  for Tab and Shift-Tab, letting the browser's default
  focus-changing behavior go through instead. This is useful to
  prevent trapping keyboard users in your editor.
  
  Without argument, this toggles the mode. With a boolean, it
  enables (true) or disables it (false). Given a number, it
  temporarily enables the mode until that number of milliseconds
  have passed or another non-Tab key is pressed.
  */
  setTabFocusMode(to) {
    if (to == null)
      this.inputState.tabFocusMode = this.inputState.tabFocusMode < 0 ? 0 : -1;
    else if (typeof to == "boolean")
      this.inputState.tabFocusMode = to ? 0 : -1;
    else if (this.inputState.tabFocusMode != 0)
      this.inputState.tabFocusMode = Date.now() + to;
  }
  /**
  Returns an extension that can be used to add DOM event handlers.
  The value should be an object mapping event names to handler
  functions. For any given event, such functions are ordered by
  extension precedence, and the first handler to return true will
  be assumed to have handled that event, and no other handlers or
  built-in behavior will be activated for it. These are registered
  on the [content element](https://codemirror.net/6/docs/ref/#view.EditorView.contentDOM), except
  for `scroll` handlers, which will be called any time the
  editor's [scroll element](https://codemirror.net/6/docs/ref/#view.EditorView.scrollDOM) or one of
  its parent nodes is scrolled.
  */
  static domEventHandlers(handlers2) {
    return ViewPlugin.define(() => ({}), { eventHandlers: handlers2 });
  }
  /**
  Create an extension that registers DOM event observers. Contrary
  to event [handlers](https://codemirror.net/6/docs/ref/#view.EditorView^domEventHandlers),
  observers can't be prevented from running by a higher-precedence
  handler returning true. They also don't prevent other handlers
  and observers from running when they return true, and should not
  call `preventDefault`.
  */
  static domEventObservers(observers2) {
    return ViewPlugin.define(() => ({}), { eventObservers: observers2 });
  }
  /**
  Create a theme extension. The first argument can be a
  [`style-mod`](https://github.com/marijnh/style-mod#documentation)
  style spec providing the styles for the theme. These will be
  prefixed with a generated class for the style.
  
  Because the selectors will be prefixed with a scope class, rule
  that directly match the editor's [wrapper
  element](https://codemirror.net/6/docs/ref/#view.EditorView.dom)—to which the scope class will be
  added—need to be explicitly differentiated by adding an `&` to
  the selector for that element—for example
  `&.cm-focused`.
  
  When `dark` is set to true, the theme will be marked as dark,
  which will cause the `&dark` rules from [base
  themes](https://codemirror.net/6/docs/ref/#view.EditorView^baseTheme) to be used (as opposed to
  `&light` when a light theme is active).
  */
  static theme(spec, options) {
    let prefix = StyleModule.newName();
    let result = [theme.of(prefix), styleModule.of(buildTheme(`.${prefix}`, spec))];
    if (options && options.dark)
      result.push(darkTheme.of(true));
    return result;
  }
  /**
  Create an extension that adds styles to the base theme. Like
  with [`theme`](https://codemirror.net/6/docs/ref/#view.EditorView^theme), use `&` to indicate the
  place of the editor wrapper element when directly targeting
  that. You can also use `&dark` or `&light` instead to only
  target editors with a dark or light theme.
  */
  static baseTheme(spec) {
    return Prec.lowest(styleModule.of(buildTheme("." + baseThemeID, spec, lightDarkIDs)));
  }
  /**
  Retrieve an editor view instance from the view's DOM
  representation.
  */
  static findFromDOM(dom) {
    var _a2;
    let content2 = dom.querySelector(".cm-content");
    let tile = content2 && Tile.get(content2) || Tile.get(dom);
    return ((_a2 = tile === null || tile === void 0 ? void 0 : tile.root) === null || _a2 === void 0 ? void 0 : _a2.view) || null;
  }
};
EditorView.styleModule = styleModule;
EditorView.inputHandler = inputHandler;
EditorView.clipboardInputFilter = clipboardInputFilter;
EditorView.clipboardOutputFilter = clipboardOutputFilter;
EditorView.scrollHandler = scrollHandler;
EditorView.focusChangeEffect = focusChangeEffect;
EditorView.perLineTextDirection = perLineTextDirection;
EditorView.exceptionSink = exceptionSink;
EditorView.updateListener = updateListener;
EditorView.editable = editable;
EditorView.mouseSelectionStyle = mouseSelectionStyle;
EditorView.dragMovesSelection = dragMovesSelection$1;
EditorView.clickAddsSelectionRange = clickAddsSelectionRange;
EditorView.decorations = decorations;
EditorView.blockWrappers = blockWrappers;
EditorView.outerDecorations = outerDecorations;
EditorView.atomicRanges = atomicRanges;
EditorView.bidiIsolatedRanges = bidiIsolatedRanges;
EditorView.scrollMargins = scrollMargins;
EditorView.darkTheme = darkTheme;
EditorView.cspNonce = /* @__PURE__ */ Facet.define({ combine: (values) => values.length ? values[0] : "" });
EditorView.contentAttributes = contentAttributes;
EditorView.editorAttributes = editorAttributes;
EditorView.lineWrapping = /* @__PURE__ */ EditorView.contentAttributes.of({ "class": "cm-lineWrapping" });
EditorView.announce = /* @__PURE__ */ StateEffect.define();
var MaxBidiLine = 4096;
var BadMeasure = {};
var CachedOrder = class _CachedOrder {
  constructor(from, to, dir, isolates, fresh, order) {
    this.from = from;
    this.to = to;
    this.dir = dir;
    this.isolates = isolates;
    this.fresh = fresh;
    this.order = order;
  }
  static update(cache, changes) {
    if (changes.empty && !cache.some((c) => c.fresh))
      return cache;
    let result = [], lastDir = cache.length ? cache[cache.length - 1].dir : Direction.LTR;
    for (let i = Math.max(0, cache.length - 10); i < cache.length; i++) {
      let entry = cache[i];
      if (entry.dir == lastDir && !changes.touchesRange(entry.from, entry.to))
        result.push(new _CachedOrder(changes.mapPos(entry.from, 1), changes.mapPos(entry.to, -1), entry.dir, entry.isolates, false, entry.order));
    }
    return result;
  }
};
function attrsFromFacet(view, facet, base2) {
  for (let sources = view.state.facet(facet), i = sources.length - 1; i >= 0; i--) {
    let source = sources[i], value = typeof source == "function" ? source(view) : source;
    if (value)
      combineAttrs(value, base2);
  }
  return base2;
}
var currentPlatform = browser.mac ? "mac" : browser.windows ? "win" : browser.linux ? "linux" : "key";
var UnicodeRegexpSupport = /x/.unicode != null ? "gu" : "g";
var baseTheme = /* @__PURE__ */ EditorView.baseTheme({
  ".cm-tooltip": {
    zIndex: 500,
    boxSizing: "border-box"
  },
  "&light .cm-tooltip": {
    border: "1px solid #bbb",
    backgroundColor: "#f5f5f5"
  },
  "&light .cm-tooltip-section:not(:first-child)": {
    borderTop: "1px solid #bbb"
  },
  "&dark .cm-tooltip": {
    backgroundColor: "#333338",
    color: "white"
  },
  ".cm-tooltip-arrow": {
    height: `${7}px`,
    width: `${7 * 2}px`,
    position: "absolute",
    zIndex: -1,
    overflow: "hidden",
    "&:before, &:after": {
      content: "''",
      position: "absolute",
      width: 0,
      height: 0,
      borderLeft: `${7}px solid transparent`,
      borderRight: `${7}px solid transparent`
    },
    ".cm-tooltip-above &": {
      bottom: `-${7}px`,
      "&:before": {
        borderTop: `${7}px solid #bbb`
      },
      "&:after": {
        borderTop: `${7}px solid #f5f5f5`,
        bottom: "1px"
      }
    },
    ".cm-tooltip-below &": {
      top: `-${7}px`,
      "&:before": {
        borderBottom: `${7}px solid #bbb`
      },
      "&:after": {
        borderBottom: `${7}px solid #f5f5f5`,
        top: "1px"
      }
    }
  },
  "&dark .cm-tooltip .cm-tooltip-arrow": {
    "&:before": {
      borderTopColor: "#333338",
      borderBottomColor: "#333338"
    },
    "&:after": {
      borderTopColor: "transparent",
      borderBottomColor: "transparent"
    }
  }
});
var GutterMarker = class extends RangeValue {
  /**
  @internal
  */
  compare(other) {
    return this == other || this.constructor == other.constructor && this.eq(other);
  }
  /**
  Compare this marker to another marker of the same type.
  */
  eq(other) {
    return false;
  }
  /**
  Called if the marker has a `toDOM` method and its representation
  was removed from a gutter.
  */
  destroy(dom) {
  }
};
GutterMarker.prototype.elementClass = "";
GutterMarker.prototype.toDOM = void 0;
GutterMarker.prototype.mapMode = MapMode.TrackBefore;
GutterMarker.prototype.startSide = GutterMarker.prototype.endSide = -1;
GutterMarker.prototype.point = true;

// node_modules/@codemirror/language/dist/index.js
var _a;
var languageDataProp = /* @__PURE__ */ new NodeProp();
function defineLanguageFacet(baseData) {
  return Facet.define({
    combine: baseData ? (values) => values.concat(baseData) : void 0
  });
}
var sublanguageProp = /* @__PURE__ */ new NodeProp();
var Language = class {
  /**
  Construct a language object. If you need to invoke this
  directly, first define a data facet with
  [`defineLanguageFacet`](https://codemirror.net/6/docs/ref/#language.defineLanguageFacet), and then
  configure your parser to [attach](https://codemirror.net/6/docs/ref/#language.languageDataProp) it
  to the language's outer syntax node.
  */
  constructor(data, parser2, extraExtensions = [], name2 = "") {
    this.data = data;
    this.name = name2;
    if (!EditorState.prototype.hasOwnProperty("tree"))
      Object.defineProperty(EditorState.prototype, "tree", { get() {
        return syntaxTree(this);
      } });
    this.parser = parser2;
    this.extension = [
      language.of(this),
      EditorState.languageData.of((state, pos, side) => {
        let top2 = topNodeAt(state, pos, side), data2 = top2.type.prop(languageDataProp);
        if (!data2)
          return [];
        let base2 = state.facet(data2), sub = top2.type.prop(sublanguageProp);
        if (sub) {
          let innerNode = top2.resolve(pos - top2.from, side);
          for (let sublang of sub)
            if (sublang.test(innerNode, state)) {
              let data3 = state.facet(sublang.facet);
              return sublang.type == "replace" ? data3 : data3.concat(base2);
            }
        }
        return base2;
      })
    ].concat(extraExtensions);
  }
  /**
  Query whether this language is active at the given position.
  */
  isActiveAt(state, pos, side = -1) {
    return topNodeAt(state, pos, side).type.prop(languageDataProp) == this.data;
  }
  /**
  Find the document regions that were parsed using this language.
  The returned regions will _include_ any nested languages rooted
  in this language, when those exist.
  */
  findRegions(state) {
    let lang = state.facet(language);
    if ((lang === null || lang === void 0 ? void 0 : lang.data) == this.data)
      return [{ from: 0, to: state.doc.length }];
    if (!lang || !lang.allowsNesting)
      return [];
    let result = [];
    let explore = (tree, from) => {
      if (tree.prop(languageDataProp) == this.data) {
        result.push({ from, to: from + tree.length });
        return;
      }
      let mount = tree.prop(NodeProp.mounted);
      if (mount) {
        if (mount.tree.prop(languageDataProp) == this.data) {
          if (mount.overlay)
            for (let r of mount.overlay)
              result.push({ from: r.from + from, to: r.to + from });
          else
            result.push({ from, to: from + tree.length });
          return;
        } else if (mount.overlay) {
          let size = result.length;
          explore(mount.tree, mount.overlay[0].from + from);
          if (result.length > size)
            return;
        }
      }
      for (let i = 0; i < tree.children.length; i++) {
        let ch = tree.children[i];
        if (ch instanceof Tree)
          explore(ch, tree.positions[i] + from);
      }
    };
    explore(syntaxTree(state), 0);
    return result;
  }
  /**
  Indicates whether this language allows nested languages. The
  default implementation returns true.
  */
  get allowsNesting() {
    return true;
  }
};
Language.setState = /* @__PURE__ */ StateEffect.define();
function topNodeAt(state, pos, side) {
  let topLang = state.facet(language), tree = syntaxTree(state).topNode;
  if (!topLang || topLang.allowsNesting) {
    for (let node = tree; node; node = node.enter(pos, side, IterMode.ExcludeBuffers))
      if (node.type.isTop)
        tree = node;
  }
  return tree;
}
var LRLanguage = class _LRLanguage extends Language {
  constructor(data, parser2, name2) {
    super(data, parser2, [], name2);
    this.parser = parser2;
  }
  /**
  Define a language from a parser.
  */
  static define(spec) {
    let data = defineLanguageFacet(spec.languageData);
    return new _LRLanguage(data, spec.parser.configure({
      props: [languageDataProp.add((type) => type.isTop ? data : void 0)]
    }), spec.name);
  }
  /**
  Create a new instance of this language with a reconfigured
  version of its parser and optionally a new name.
  */
  configure(options, name2) {
    return new _LRLanguage(this.data, this.parser.configure(options), name2 || this.name);
  }
  get allowsNesting() {
    return this.parser.hasWrappers();
  }
};
function syntaxTree(state) {
  let field = state.field(Language.state, false);
  return field ? field.tree : Tree.empty;
}
var DocInput = class {
  /**
  Create an input object for the given document.
  */
  constructor(doc2) {
    this.doc = doc2;
    this.cursorPos = 0;
    this.string = "";
    this.cursor = doc2.iter();
  }
  get length() {
    return this.doc.length;
  }
  syncTo(pos) {
    this.string = this.cursor.next(pos - this.cursorPos).value;
    this.cursorPos = pos + this.string.length;
    return this.cursorPos - this.string.length;
  }
  chunk(pos) {
    this.syncTo(pos);
    return this.string;
  }
  get lineChunks() {
    return true;
  }
  read(from, to) {
    let stringStart = this.cursorPos - this.string.length;
    if (from < stringStart || to >= this.cursorPos)
      return this.doc.sliceString(from, to);
    else
      return this.string.slice(from - stringStart, to - stringStart);
  }
};
var currentContext = null;
var ParseContext = class _ParseContext {
  constructor(parser2, state, fragments = [], tree, treeLen, viewport, skipped, scheduleOn) {
    this.parser = parser2;
    this.state = state;
    this.fragments = fragments;
    this.tree = tree;
    this.treeLen = treeLen;
    this.viewport = viewport;
    this.skipped = skipped;
    this.scheduleOn = scheduleOn;
    this.parse = null;
    this.tempSkipped = [];
  }
  /**
  @internal
  */
  static create(parser2, state, viewport) {
    return new _ParseContext(parser2, state, [], Tree.empty, 0, viewport, [], null);
  }
  startParse() {
    return this.parser.startParse(new DocInput(this.state.doc), this.fragments);
  }
  /**
  @internal
  */
  work(until, upto) {
    if (upto != null && upto >= this.state.doc.length)
      upto = void 0;
    if (this.tree != Tree.empty && this.isDone(upto !== null && upto !== void 0 ? upto : this.state.doc.length)) {
      this.takeTree();
      return true;
    }
    return this.withContext(() => {
      var _a2;
      if (typeof until == "number") {
        let endTime = Date.now() + until;
        until = () => Date.now() > endTime;
      }
      if (!this.parse)
        this.parse = this.startParse();
      if (upto != null && (this.parse.stoppedAt == null || this.parse.stoppedAt > upto) && upto < this.state.doc.length)
        this.parse.stopAt(upto);
      for (; ; ) {
        let done = this.parse.advance();
        if (done) {
          this.fragments = this.withoutTempSkipped(TreeFragment.addTree(done, this.fragments, this.parse.stoppedAt != null));
          this.treeLen = (_a2 = this.parse.stoppedAt) !== null && _a2 !== void 0 ? _a2 : this.state.doc.length;
          this.tree = done;
          this.parse = null;
          if (this.treeLen < (upto !== null && upto !== void 0 ? upto : this.state.doc.length))
            this.parse = this.startParse();
          else
            return true;
        }
        if (until())
          return false;
      }
    });
  }
  /**
  @internal
  */
  takeTree() {
    let pos, tree;
    if (this.parse && (pos = this.parse.parsedPos) >= this.treeLen) {
      if (this.parse.stoppedAt == null || this.parse.stoppedAt > pos)
        this.parse.stopAt(pos);
      this.withContext(() => {
        while (!(tree = this.parse.advance())) {
        }
      });
      this.treeLen = pos;
      this.tree = tree;
      this.fragments = this.withoutTempSkipped(TreeFragment.addTree(this.tree, this.fragments, true));
      this.parse = null;
    }
  }
  withContext(f) {
    let prev = currentContext;
    currentContext = this;
    try {
      return f();
    } finally {
      currentContext = prev;
    }
  }
  withoutTempSkipped(fragments) {
    for (let r; r = this.tempSkipped.pop(); )
      fragments = cutFragments(fragments, r.from, r.to);
    return fragments;
  }
  /**
  @internal
  */
  changes(changes, newState) {
    let { fragments, tree, treeLen, viewport, skipped } = this;
    this.takeTree();
    if (!changes.empty) {
      let ranges = [];
      changes.iterChangedRanges((fromA, toA, fromB, toB) => ranges.push({ fromA, toA, fromB, toB }));
      fragments = TreeFragment.applyChanges(fragments, ranges);
      tree = Tree.empty;
      treeLen = 0;
      viewport = { from: changes.mapPos(viewport.from, -1), to: changes.mapPos(viewport.to, 1) };
      if (this.skipped.length) {
        skipped = [];
        for (let r of this.skipped) {
          let from = changes.mapPos(r.from, 1), to = changes.mapPos(r.to, -1);
          if (from < to)
            skipped.push({ from, to });
        }
      }
    }
    return new _ParseContext(this.parser, newState, fragments, tree, treeLen, viewport, skipped, this.scheduleOn);
  }
  /**
  @internal
  */
  updateViewport(viewport) {
    if (this.viewport.from == viewport.from && this.viewport.to == viewport.to)
      return false;
    this.viewport = viewport;
    let startLen = this.skipped.length;
    for (let i = 0; i < this.skipped.length; i++) {
      let { from, to } = this.skipped[i];
      if (from < viewport.to && to > viewport.from) {
        this.fragments = cutFragments(this.fragments, from, to);
        this.skipped.splice(i--, 1);
      }
    }
    if (this.skipped.length >= startLen)
      return false;
    this.reset();
    return true;
  }
  /**
  @internal
  */
  reset() {
    if (this.parse) {
      this.takeTree();
      this.parse = null;
    }
  }
  /**
  Notify the parse scheduler that the given region was skipped
  because it wasn't in view, and the parse should be restarted
  when it comes into view.
  */
  skipUntilInView(from, to) {
    this.skipped.push({ from, to });
  }
  /**
  Returns a parser intended to be used as placeholder when
  asynchronously loading a nested parser. It'll skip its input and
  mark it as not-really-parsed, so that the next update will parse
  it again.
  
  When `until` is given, a reparse will be scheduled when that
  promise resolves.
  */
  static getSkippingParser(until) {
    return new class extends Parser {
      createParse(input, fragments, ranges) {
        let from = ranges[0].from, to = ranges[ranges.length - 1].to;
        let parser2 = {
          parsedPos: from,
          advance() {
            let cx = currentContext;
            if (cx) {
              for (let r of ranges)
                cx.tempSkipped.push(r);
              if (until)
                cx.scheduleOn = cx.scheduleOn ? Promise.all([cx.scheduleOn, until]) : until;
            }
            this.parsedPos = to;
            return new Tree(NodeType.none, [], [], to - from);
          },
          stoppedAt: null,
          stopAt() {
          }
        };
        return parser2;
      }
    }();
  }
  /**
  @internal
  */
  isDone(upto) {
    upto = Math.min(upto, this.state.doc.length);
    let frags = this.fragments;
    return this.treeLen >= upto && frags.length && frags[0].from == 0 && frags[0].to >= upto;
  }
  /**
  Get the context for the current parse, or `null` if no editor
  parse is in progress.
  */
  static get() {
    return currentContext;
  }
};
function cutFragments(fragments, from, to) {
  return TreeFragment.applyChanges(fragments, [{ fromA: from, toA: to, fromB: from, toB: to }]);
}
var LanguageState = class _LanguageState {
  constructor(context) {
    this.context = context;
    this.tree = context.tree;
  }
  apply(tr) {
    if (!tr.docChanged && this.tree == this.context.tree)
      return this;
    let newCx = this.context.changes(tr.changes, tr.state);
    let upto = this.context.treeLen == tr.startState.doc.length ? void 0 : Math.max(tr.changes.mapPos(this.context.treeLen), newCx.viewport.to);
    if (!newCx.work(20, upto))
      newCx.takeTree();
    return new _LanguageState(newCx);
  }
  static init(state) {
    let vpTo = Math.min(3e3, state.doc.length);
    let parseState = ParseContext.create(state.facet(language).parser, state, { from: 0, to: vpTo });
    if (!parseState.work(20, vpTo))
      parseState.takeTree();
    return new _LanguageState(parseState);
  }
};
Language.state = /* @__PURE__ */ StateField.define({
  create: LanguageState.init,
  update(value, tr) {
    for (let e of tr.effects)
      if (e.is(Language.setState))
        return e.value;
    if (tr.startState.facet(language) != tr.state.facet(language))
      return LanguageState.init(tr.state);
    return value.apply(tr);
  }
});
var requestIdle = (callback) => {
  let timeout = setTimeout(
    () => callback(),
    500
    /* Work.MaxPause */
  );
  return () => clearTimeout(timeout);
};
if (typeof requestIdleCallback != "undefined")
  requestIdle = (callback) => {
    let idle = -1, timeout = setTimeout(
      () => {
        idle = requestIdleCallback(callback, {
          timeout: 500 - 100
          /* Work.MinPause */
        });
      },
      100
      /* Work.MinPause */
    );
    return () => idle < 0 ? clearTimeout(timeout) : cancelIdleCallback(idle);
  };
var isInputPending = typeof navigator != "undefined" && ((_a = navigator.scheduling) === null || _a === void 0 ? void 0 : _a.isInputPending) ? () => navigator.scheduling.isInputPending() : null;
var parseWorker = /* @__PURE__ */ ViewPlugin.fromClass(class ParseWorker {
  constructor(view) {
    this.view = view;
    this.working = null;
    this.workScheduled = 0;
    this.chunkEnd = -1;
    this.chunkBudget = -1;
    this.work = this.work.bind(this);
    this.scheduleWork();
  }
  update(update) {
    let cx = this.view.state.field(Language.state).context;
    if (cx.updateViewport(update.view.viewport) || this.view.viewport.to > cx.treeLen)
      this.scheduleWork();
    if (update.docChanged || update.selectionSet) {
      if (this.view.hasFocus)
        this.chunkBudget += 50;
      this.scheduleWork();
    }
    this.checkAsyncSchedule(cx);
  }
  scheduleWork() {
    if (this.working)
      return;
    let { state } = this.view, field = state.field(Language.state);
    if (field.tree != field.context.tree || !field.context.isDone(state.doc.length))
      this.working = requestIdle(this.work);
  }
  work(deadline) {
    this.working = null;
    let now = Date.now();
    if (this.chunkEnd < now && (this.chunkEnd < 0 || this.view.hasFocus)) {
      this.chunkEnd = now + 3e4;
      this.chunkBudget = 3e3;
    }
    if (this.chunkBudget <= 0)
      return;
    let { state, viewport: { to: vpTo } } = this.view, field = state.field(Language.state);
    if (field.tree == field.context.tree && field.context.isDone(
      vpTo + 1e5
      /* Work.MaxParseAhead */
    ))
      return;
    let endTime = Date.now() + Math.min(this.chunkBudget, 100, deadline && !isInputPending ? Math.max(25, deadline.timeRemaining() - 5) : 1e9);
    let viewportFirst = field.context.treeLen < vpTo && state.doc.length > vpTo + 1e3;
    let done = field.context.work(() => {
      return isInputPending && isInputPending() || Date.now() > endTime;
    }, vpTo + (viewportFirst ? 0 : 1e5));
    this.chunkBudget -= Date.now() - now;
    if (done || this.chunkBudget <= 0) {
      field.context.takeTree();
      this.view.dispatch({ effects: Language.setState.of(new LanguageState(field.context)) });
    }
    if (this.chunkBudget > 0 && !(done && !viewportFirst))
      this.scheduleWork();
    this.checkAsyncSchedule(field.context);
  }
  checkAsyncSchedule(cx) {
    if (cx.scheduleOn) {
      this.workScheduled++;
      cx.scheduleOn.then(() => this.scheduleWork()).catch((err) => logException(this.view.state, err)).then(() => this.workScheduled--);
      cx.scheduleOn = null;
    }
  }
  destroy() {
    if (this.working)
      this.working();
  }
  isWorking() {
    return !!(this.working || this.workScheduled > 0);
  }
}, {
  eventHandlers: { focus() {
    this.scheduleWork();
  } }
});
var language = /* @__PURE__ */ Facet.define({
  combine(languages) {
    return languages.length ? languages[0] : null;
  },
  enables: (language2) => [
    Language.state,
    parseWorker,
    EditorView.contentAttributes.compute([language2], (state) => {
      let lang = state.facet(language2);
      return lang && lang.name ? { "data-language": lang.name } : {};
    })
  ]
});
var LanguageSupport = class {
  /**
  Create a language support object.
  */
  constructor(language2, support = []) {
    this.language = language2;
    this.support = support;
    this.extension = [language2, support];
  }
};
var indentNodeProp = /* @__PURE__ */ new NodeProp();
function bracketedAligned(context) {
  let tree = context.node;
  let openToken = tree.childAfter(tree.from), last = tree.lastChild;
  if (!openToken)
    return null;
  let sim = context.options.simulateBreak;
  let openLine = context.state.doc.lineAt(openToken.from);
  let lineEnd = sim == null || sim <= openLine.from ? openLine.to : Math.min(openLine.to, sim);
  for (let pos = openToken.to; ; ) {
    let next = tree.childAfter(pos);
    if (!next || next == last)
      return null;
    if (!next.type.isSkipped) {
      if (next.from >= lineEnd)
        return null;
      let space = /^ */.exec(openLine.text.slice(openToken.to - openLine.from))[0].length;
      return { from: openToken.from, to: openToken.to + space };
    }
    pos = next.to;
  }
}
function delimitedIndent({ closing, align = true, units = 1 }) {
  return (context) => delimitedStrategy(context, align, units, closing);
}
function delimitedStrategy(context, align, units, closing, closedAt) {
  let after = context.textAfter, space = after.match(/^\s*/)[0].length;
  let closed = closing && after.slice(space, space + closing.length) == closing || closedAt == context.pos + space;
  let aligned = align ? bracketedAligned(context) : null;
  if (aligned)
    return closed ? context.column(aligned.from) : context.column(aligned.to);
  return context.baseIndent + (closed ? 0 : context.unit * units);
}
var flatIndent = (context) => context.baseIndent;
function continuedIndent({ except, units = 1 } = {}) {
  return (context) => {
    let matchExcept = except && except.test(context.textAfter);
    return context.baseIndent + (matchExcept ? 0 : units * context.unit);
  };
}
var foldNodeProp = /* @__PURE__ */ new NodeProp();
function foldInside(node) {
  let first = node.firstChild, last = node.lastChild;
  return first && first.to < last.from ? { from: first.to, to: last.type.isError ? node.to : last.from } : null;
}
var HighlightStyle = class _HighlightStyle {
  constructor(specs, options) {
    this.specs = specs;
    let modSpec;
    function def(spec) {
      let cls = StyleModule.newName();
      (modSpec || (modSpec = /* @__PURE__ */ Object.create(null)))["." + cls] = spec;
      return cls;
    }
    const all = typeof options.all == "string" ? options.all : options.all ? def(options.all) : void 0;
    const scopeOpt = options.scope;
    this.scope = scopeOpt instanceof Language ? (type) => type.prop(languageDataProp) == scopeOpt.data : scopeOpt ? (type) => type == scopeOpt : void 0;
    this.style = tagHighlighter(specs.map((style) => ({
      tag: style.tag,
      class: style.class || def(Object.assign({}, style, { tag: null }))
    })), {
      all
    }).style;
    this.module = modSpec ? new StyleModule(modSpec) : null;
    this.themeType = options.themeType;
  }
  /**
  Create a highlighter style that associates the given styles to
  the given tags. The specs must be objects that hold a style tag
  or array of tags in their `tag` property, and either a single
  `class` property providing a static CSS class (for highlighter
  that rely on external styling), or a
  [`style-mod`](https://github.com/marijnh/style-mod#documentation)-style
  set of CSS properties (which define the styling for those tags).
  
  The CSS rules created for a highlighter will be emitted in the
  order of the spec's properties. That means that for elements that
  have multiple tags associated with them, styles defined further
  down in the list will have a higher CSS precedence than styles
  defined earlier.
  */
  static define(specs, options) {
    return new _HighlightStyle(specs, options || {});
  }
};
var defaultHighlightStyle = /* @__PURE__ */ HighlightStyle.define([
  {
    tag: tags.meta,
    color: "#404740"
  },
  {
    tag: tags.link,
    textDecoration: "underline"
  },
  {
    tag: tags.heading,
    textDecoration: "underline",
    fontWeight: "bold"
  },
  {
    tag: tags.emphasis,
    fontStyle: "italic"
  },
  {
    tag: tags.strong,
    fontWeight: "bold"
  },
  {
    tag: tags.strikethrough,
    textDecoration: "line-through"
  },
  {
    tag: tags.keyword,
    color: "#708"
  },
  {
    tag: [tags.atom, tags.bool, tags.url, tags.contentSeparator, tags.labelName],
    color: "#219"
  },
  {
    tag: [tags.literal, tags.inserted],
    color: "#164"
  },
  {
    tag: [tags.string, tags.deleted],
    color: "#a11"
  },
  {
    tag: [tags.regexp, tags.escape, /* @__PURE__ */ tags.special(tags.string)],
    color: "#e40"
  },
  {
    tag: /* @__PURE__ */ tags.definition(tags.variableName),
    color: "#00f"
  },
  {
    tag: /* @__PURE__ */ tags.local(tags.variableName),
    color: "#30a"
  },
  {
    tag: [tags.typeName, tags.namespace],
    color: "#085"
  },
  {
    tag: tags.className,
    color: "#167"
  },
  {
    tag: [/* @__PURE__ */ tags.special(tags.variableName), tags.macroName],
    color: "#256"
  },
  {
    tag: /* @__PURE__ */ tags.definition(tags.propertyName),
    color: "#00c"
  },
  {
    tag: tags.comment,
    color: "#940"
  },
  {
    tag: tags.invalid,
    color: "#f00"
  }
]);
var noTokens = /* @__PURE__ */ Object.create(null);
var typeArray = [NodeType.none];
var warned = [];
var byTag = /* @__PURE__ */ Object.create(null);
var defaultTable = /* @__PURE__ */ Object.create(null);
for (let [legacyName, name2] of [
  ["variable", "variableName"],
  ["variable-2", "variableName.special"],
  ["string-2", "string.special"],
  ["def", "variableName.definition"],
  ["tag", "tagName"],
  ["attribute", "attributeName"],
  ["type", "typeName"],
  ["builtin", "variableName.standard"],
  ["qualifier", "modifier"],
  ["error", "invalid"],
  ["header", "heading"],
  ["property", "propertyName"]
])
  defaultTable[legacyName] = /* @__PURE__ */ createTokenType(noTokens, name2);
function warnForPart(part, msg) {
  if (warned.indexOf(part) > -1)
    return;
  warned.push(part);
  console.warn(msg);
}
function createTokenType(extra, tagStr) {
  let tags$1 = [];
  for (let name3 of tagStr.split(" ")) {
    let found = [];
    for (let part of name3.split(".")) {
      let value = extra[part] || tags[part];
      if (!value) {
        warnForPart(part, `Unknown highlighting tag ${part}`);
      } else if (typeof value == "function") {
        if (!found.length)
          warnForPart(part, `Modifier ${part} used at start of tag`);
        else
          found = found.map(value);
      } else {
        if (found.length)
          warnForPart(part, `Tag ${part} used as modifier`);
        else
          found = Array.isArray(value) ? value : [value];
      }
    }
    for (let tag of found)
      tags$1.push(tag);
  }
  if (!tags$1.length)
    return 0;
  let name2 = tagStr.replace(/ /g, "_"), key = name2 + " " + tags$1.map((t2) => t2.id);
  let known = byTag[key];
  if (known)
    return known.id;
  let type = byTag[key] = NodeType.define({
    id: typeArray.length,
    name: name2,
    props: [styleTags({ [name2]: tags$1 })]
  });
  typeArray.push(type);
  return type.id;
}
var marks = {
  rtl: /* @__PURE__ */ Decoration.mark({ class: "cm-iso", inclusive: true, attributes: { dir: "rtl" }, bidiIsolate: Direction.RTL }),
  ltr: /* @__PURE__ */ Decoration.mark({ class: "cm-iso", inclusive: true, attributes: { dir: "ltr" }, bidiIsolate: Direction.LTR }),
  auto: /* @__PURE__ */ Decoration.mark({ class: "cm-iso", inclusive: true, attributes: { dir: "auto" }, bidiIsolate: null })
};

// node_modules/@codemirror/lang-cpp/dist/index.js
var cppLanguage = /* @__PURE__ */ LRLanguage.define({
  name: "cpp",
  parser: /* @__PURE__ */ parser.configure({
    props: [
      /* @__PURE__ */ indentNodeProp.add({
        IfStatement: /* @__PURE__ */ continuedIndent({ except: /^\s*({|else\b)/ }),
        TryStatement: /* @__PURE__ */ continuedIndent({ except: /^\s*({|catch)\b/ }),
        LabeledStatement: flatIndent,
        CaseStatement: (context) => context.baseIndent + context.unit,
        BlockComment: () => null,
        CompoundStatement: /* @__PURE__ */ delimitedIndent({ closing: "}" }),
        Statement: /* @__PURE__ */ continuedIndent({ except: /^{/ })
      }),
      /* @__PURE__ */ foldNodeProp.add({
        "DeclarationList CompoundStatement EnumeratorList FieldDeclarationList InitializerList": foldInside,
        BlockComment(tree) {
          return { from: tree.from + 2, to: tree.to - 2 };
        }
      })
    ]
  }),
  languageData: {
    commentTokens: { line: "//", block: { open: "/*", close: "*/" } },
    indentOnInput: /^\s*(?:case |default:|\{|\})$/,
    closeBrackets: { stringPrefixes: ["L", "u", "U", "u8", "LR", "UR", "uR", "u8R", "R"] }
  }
});
function cpp() {
  return new LanguageSupport(cppLanguage);
}

// index.js
var index_default = cpp;
export {
  index_default as default
};
