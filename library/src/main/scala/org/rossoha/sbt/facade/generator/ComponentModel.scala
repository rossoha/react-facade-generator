package org.rossoha.sbt.facade.generator

object ComponentModel {

  sealed trait PropType

  /**
   *  PropTypes not specified
   *  js.Any
   */
  case object AnyType extends PropType

  /**
   *  PropTypes.array
   *  Array of js.Any
   */
  case object ArrayType extends PropType

  /**
   *  PropTypes.bool
   */
  case object BooleanType extends PropType

  /**
   *  PropTypes.string
   */
  case object StringType extends PropType

  /**
   *  PropTypes.number
   */
  case object NumberType extends PropType

  /**
   *  PropTypes.func
   */
  case object FuncType extends PropType

  /**
   *  PropTypes.object
   */
  case object ObjectType extends PropType

  /**
   *  PropTypes.symbol
   */
  case object SymbolType extends PropType

  /**
   *  PropTypes.element
   */
  case object ElementType extends PropType

  /**
   *  PropTypes.node
   */
  case object NodeType extends PropType

  /**
   *  PropTypes.instanceOf(name)
   */
  // case class ClassType(name: String) extends PropType

  /**
   *  One permitted value in an enum - assumed to be a String
   *  TODO - what is computed?
   */
  case class EnumValue(value: String, computed: Boolean)

  /**
   *  PropTypes.oneOf([values1, values2, ...])
   */
  case class EnumType(values: Set[EnumValue]) extends PropType

  /**
   *  PropTypes.oneOfType([types1, types2, ...])
   */
  case class UnionType(types: Set[PropType]) extends PropType

  /**
   *  PropTypes.arrayOf(elementType)
   */
  case class ArrayOfType(elementType: PropType) extends PropType

  // TODO - need an example of docgen output
  /**
   * PropTypes.objectOf(PropTypes.valueType)
   * Map from string to valueType
   */
  // case class StringMapType(valueType: PropType) extends PropType

  /**
   *  PropTypes.shape({
   *    key1: fieldType1,
   *    key2: fieldType2,
   *    ...
   *  }
   */
  case class StructuralType(fieldTypes: Map[String, PropType]) extends PropType

  /**
   *  PropTypes uses:
   *
   *  customProp: function(props, propName, componentName) { ... }
   *
   *  with some validation function.
   *
   *  Docgen seems to use name "custom" and a "raw" field with the name of the function.
   *  At the very least we can use this to produce a js.Any property, or detect
   *  special cases like the "unsupportedProp" value MUI seems to use to indicate
   *  a property shouldn't be used
   */
  case class CustomType(raw: String) extends PropType

  // Following PropTypes are synthetic - we produce these by processing imported PropTypes,
  // e.g. by adding information about functions based on heuristics or knowledge of base components

  /**
   * A function with known scala and js type, and code to convert scala type to js type on assignment to Props
   */
  case class KnownFuncType(scalaType: String, jsType: String, assignment: String => String)
      extends PropType

  /**
   * Accepts a style, of type mui.style.Style
   */
  case object StyleType extends PropType

  case class Component(
    description: String,
    displayName: String,
    props: List[(String, Prop)] = List.empty
  )

  case class Value(
    value: String,
    computed: Boolean
  )

  case class Prop(
    propType: PropType,
    required: Boolean,
    description: String,
    defaultValue: Option[Value]
  )

  case class ImportData(
    module: String,
    name: String
  )

  case class ComponentData(
    component: Component,
    importData: ImportData,
    functional: Boolean
  )

}
