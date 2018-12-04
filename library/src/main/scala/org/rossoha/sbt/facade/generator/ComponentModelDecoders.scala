package org.rossoha.sbt.facade.generator

import io.circe._
import org.rossoha.sbt.facade.generator.ComponentModel._

object ComponentModelDecoders {

  def pDecoder[A](name: String, a: A): Decoder[A] =
    Decoder.instance(
      c =>
        c.downField("name")
          .as[String]
          .filterOrElse(_ == name, DecodingFailure(s"Not a $name", c.history))
          .map(_ => a)
    )

  implicit val anyTypeDecoder: Decoder[AnyType.type]         = pDecoder("any", AnyType)
  implicit val arrayTypeDecoder: Decoder[ArrayType.type]     = pDecoder("array", ArrayType)
  implicit val booleanTypeDecoder: Decoder[BooleanType.type] = pDecoder("bool", BooleanType)
  implicit val stringTypeDecoder: Decoder[StringType.type]   = pDecoder("string", StringType)
  implicit val numberTypeDecoder: Decoder[NumberType.type]   = pDecoder("number", NumberType)
  implicit val funcTypeDecoder: Decoder[FuncType.type]       = pDecoder("func", FuncType)
  implicit val objectTypeDecoder: Decoder[ObjectType.type]   = pDecoder("object", ObjectType)
  implicit val symbolTypeDecoder: Decoder[SymbolType.type]   = pDecoder("symbol", SymbolType)
  implicit val elementTypeDecoder: Decoder[ElementType.type] = pDecoder("element", ElementType)
  implicit val nodeTypeDecoder: Decoder[NodeType.type]       = pDecoder("node", NodeType)

  private def withoutQuotes(s: String): String =
    if (s.length > 1 && (s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith(
          "\""
        ))) {
      s.substring(1, s.length - 1)
    } else {
      s
    }

  implicit val enumValueDecoder: Decoder[EnumValue] = Decoder.instance(
    c =>
      for {
        value    <- c.downField("value").as[String]
        computed <- c.downField("computed").as[Boolean]
      } yield {
        EnumValue(withoutQuotes(value), computed)
    }
  )

  implicit val enumTypeDecoder: Decoder[EnumType] = Decoder.instance(
    c =>
      for {
        _ <- c.downField("name")
              .as[String]
              .filterOrElse(_ == "enum", DecodingFailure(s"Not an enum", c.history))
        values <- c.downField("value").as[Set[EnumValue]]
      } yield {
        EnumType(values)
    }
  )

  implicit val unionTypeDecoder: Decoder[UnionType] = Decoder.instance(
    c =>
      for {
        _ <- c.downField("name")
              .as[String]
              .filterOrElse(_ == "union", DecodingFailure(s"Not a union", c.history))
        values <- c.downField("value").as[Set[PropType]]
      } yield {
        UnionType(values)
    }
  )

  implicit val arrayOfTypeDecoder: Decoder[ArrayOfType] = Decoder.instance(
    c =>
      for {
        _ <- c.downField("name")
              .as[String]
              .filterOrElse(_ == "arrayOf", DecodingFailure(s"Not an arrayOf", c.history))
        value <- c.downField("value").as[PropType]
      } yield {
        ArrayOfType(value)
    }
  )

  implicit val structuralTypeDecoder: Decoder[StructuralType] = Decoder.instance(
    c =>
      for {
        _ <- c.downField("name")
              .as[String]
              .filterOrElse(_ == "shape",
                            DecodingFailure(s"Not a shape (structural type)", c.history))
        fieldTypes <- c.downField("value").as[Map[String, PropType]]
      } yield {
        StructuralType(fieldTypes)
    }
  )

  implicit val customTypeDecoder: Decoder[CustomType] = Decoder.instance(
    c =>
      for {
        _ <- c.downField("name")
              .as[String]
              .filterOrElse(_ == "custom", DecodingFailure(s"Not a custom prop type", c.history))
        raw <- c.downField("raw").as[String]
      } yield {
        CustomType(raw)
    }
  )

  // TODO there must be a neater way to do this...
  implicit val propTypeDecoder: Decoder[PropType] =
    anyTypeDecoder
      .map(a => a: PropType)
      .or(arrayTypeDecoder.map(a => a: PropType))
      .or(booleanTypeDecoder.map(a => a: PropType))
      .or(stringTypeDecoder.map(a => a: PropType))
      .or(numberTypeDecoder.map(a => a: PropType))
      .or(funcTypeDecoder.map(a => a: PropType))
      .or(objectTypeDecoder.map(a => a: PropType))
      .or(symbolTypeDecoder.map(a => a: PropType))
      .or(elementTypeDecoder.map(a => a: PropType))
      .or(nodeTypeDecoder.map(a => a: PropType))
      .or(enumTypeDecoder.map(a => a: PropType))
      .or(unionTypeDecoder.map(a => a: PropType))
      .or(arrayOfTypeDecoder.map(a => a: PropType))
      .or(structuralTypeDecoder.map(a => a: PropType))
      .or(customTypeDecoder.map(a => a: PropType))

  lazy implicit val componentDecoder: Decoder[Component] = Decoder.instance(
    c =>
      for {
        d <- c.downField("description").as[String]
        n <- c.downField("displayName").as[String]
        p <- c.downField("props")
              .as[Option[Map[String, Prop]]]
              .map(optMap => optMap.map(_.toList.sortBy(_._1)).getOrElse(List.empty))
      } yield {
        Component(d, n, p)
    }
  )

  lazy implicit val valueDecoder: Decoder[Value] = Decoder.instance(
    c =>
      for {
        v <- c.downField("value").as[String].map(withoutQuotes)
        c <- c.downField("computed").as[Boolean]
      } yield {
        Value(v, c)
    }
  )

  // Custom codec to avoid awkward `type` field
  // implicit val encodeProp: Encoder[Prop] =
  //   Encoder.forProduct4("type", "required", "description", "defaultValue")(u => (u.propType, u.required, u.description, u.defaultValue))
  implicit val decodeProp: Decoder[Prop] =
    Decoder.forProduct4("type", "required", "description", "defaultValue")(Prop.apply)

}
