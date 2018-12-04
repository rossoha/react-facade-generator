package org.rossoha.sbt.facade.generator
import org.rossoha.sbt.facade.generator.ComponentModel._

trait DocGenContext {
  def processComponent(all: Map[String, Component],
                       path: String,
                       c: Component,
                       module: Option[String] = None): Option[ComponentData]
}

object DocGenContext {

  val callbackFuncType =
    KnownFuncType("Callback", "scalajs.js.Function0[Unit]", (n: String) => s"$n.toJsFn")

  def eventFuncType(event: String) =
    KnownFuncType(s"$event => Callback",
                  s"scalajs.js.Function1[$event, Unit]",
                  (n: String) => s"(e: $event) => $n(e).runNow()")

  case class FuncData(scalaType: String, jsType: String, assignment: String)

  object MaterialUI extends DocGenContext {

    def extractImportData(pathRaw: String, c: Component, module: Option[String]): ImportData =
      //TODO this might be more general, but for now the only special case
      //for a public component is MuiThemeProvider
      // // Get rid of windows paths, split up
      // List[String] path = pathRaw.replaceAllLiterally("\\", "/").split("/").toList

      // path match {
      //   // Ingore internal components
      //   case "packages" :: "material-ui" :: "src" :: "internal" :: _ => None

      //   // Many components are in their own folder
      //   case "packages" :: "material-ui" :: "src" :: c.displayName :: s"${c.displayName}.js"
      //     => Some(s"@material-ui/core/${c.displayName}")

      //   // Some components are in another module - e.g. styles for MuitThemeProvider
      //   case "packages" :: "material-ui" :: "src" :: module :: s"${c.displayName}.js"
      //     => Some(s"@material-ui/core/$module/${c.displayName}")

      // }
      if (module.isDefined) {
        ImportData(s"${module.get}/$pathRaw", "JSImport.Default")
      } else if (c.displayName == "MuiThemeProvider") {
        ImportData(s"@material-ui/core/styles", s""""${c.displayName}"""")
      } else {
        ImportData(s"@material-ui/core/${c.displayName}", "JSImport.Default")
      }

    def isFunctional(pathRaw: String, c: Component): Boolean = c.displayName != "MuiThemeProvider"

    def processComponent(all: Map[String, Component],
                         path: String,
                         c: Component,
                         module: Option[String]): Option[ComponentData] = {

      val importData = extractImportData(path, c, module)

      val functional = isFunctional(path, c)

      if (c.description.contains("@ignore")) {
        println("Ignoring " + c.displayName)
        None
      } else {
        Some(
          ComponentData(
            component = c.copy(
              props = propsIncludingInheritance(all, c).map {
                case (name, prop) => transformProp(c, name, prop)
              }.filter {
                case (name, prop) => useProp(c, name, prop)
              }
            ),
            importData = importData,
            functional = functional
          )
        )
      }
    }

    def propsIncludingInheritance(all: Map[String, Component],
                                  c: Component): List[(String, Prop)] = {

      val allPlusDocgen: Map[String, Component] = all ++ Map(
        "DOCGEN_OnClickBase" -> Component(
          "DocGen component to add onClick event",
          "DOCGEN_OnClickBase",
          List(
            "onClick" -> Prop(
              FuncType,
              false,
              "ReactMouseEvent on click",
              None
            )
          )
        ),
        "DOCGEN_children" -> Component(
          "DocGen component to add children",
          "DOCGEN_Children",
          List(
            "children" -> Prop(
              NodeType,
              false,
              "React children",
              None
            )
          )
        )
      )

      def withDefaultMappings(m: Map[String, Prop], d: List[(String, Prop)]): Map[String, Prop] =
        d.foldLeft(m) {
          case (c, (name, prop)) =>
            if (c.isDefinedAt(name)) {
              c
            } else {
              c.updated(name, prop)
            }
        }

      def additionalPropsFrom(components: String*): List[(String, Prop)] = {
        val existingProps = Map(c.props: _*)

        val additionalProps =
          components.toList.flatMap(
            component =>
              allPlusDocgen.values
                .find(_.displayName == component)
                .map(
                  _.props.map {
                    case (n, p) => (component, n, p)
                  }
                )
                .getOrElse(Nil)
          )

        // TODO - refactor so that we add actual inherited props first, then DOCGEN ones without modifying description, then universal props (currently key).

        val updatedProps = additionalProps
          .foldLeft(existingProps) {
            case (props, (component, name, additionalProp)) =>
              if (props.isDefinedAt(name)) {
                props
              } else {
                props.updated(
                  name,
                  additionalProp.copy(
                    description = additionalProp.description + "\nPassed to " + component
                  )
                )
              }
          }

        val updatedPropsWithDefaults =
          withDefaultMappings(
            updatedProps,
            List(
              //                                "style" -> Prop(StyleType, false, "React element CSS style", None),
              "key" -> Prop(StringType, false, "React key", None)
            )
          )
        // if (updatedProps.isDefinedAt("key")) {
        //   updatedProps
        // } else {
        //   updatedProps.updated("key", Prop(StringType, false, "React key", None))
        // }

        updatedPropsWithDefaults.toList.sortBy(_._1)
      }

      c match {

        case Component(_, "Button", _)                 => additionalPropsFrom("ButtonBase")
        case Component(_, "BottomNavigationAction", _) => additionalPropsFrom("ButtonBase")
        case Component(_, "IconButton", _)             => additionalPropsFrom("ButtonBase")
        case Component(_, "StepButton", _)             => additionalPropsFrom("ButtonBase")
        case Component(_, "ExpansionPanelSummary", _)  => additionalPropsFrom("ButtonBase")
        case Component(_, "Tab", _)                    => additionalPropsFrom("ButtonBase")
        case Component(_, "TableSortLabel", _)         => additionalPropsFrom("ButtonBase")
        case Component(_, "CardActionArea", _)         => additionalPropsFrom("ButtonBase")

        case Component(_, "AppBar", _)          => additionalPropsFrom("Paper")
        case Component(_, "Card", _)            => additionalPropsFrom("Paper", "DOCGEN_Children")
        case Component(_, "ExpansionPanel", _)  => additionalPropsFrom("Paper")
        case Component(_, "MobileStepper", _)   => additionalPropsFrom("Paper")
        case Component(_, "SnackbarContent", _) => additionalPropsFrom("Paper")
        case Component(_, "Stepper", _)         => additionalPropsFrom("Paper")

        case Component(_, "InputLabel", _) => additionalPropsFrom("FormLabel")

        //Note that this is transitive - Menu is only described as inheriting from
        //Popover, but Popover inerits from Modal
        case Component(_, "Menu", _) => additionalPropsFrom("Popover", "Modal")

        case Component(_, "MenuItem", _) => additionalPropsFrom("ListItem", "DOCGEN_OnClickBase")

        case Component(_, "ListItem", _) => additionalPropsFrom("DOCGEN_OnClickBase")

        case Component(_, "MenuList", _) => additionalPropsFrom("List")

        case Component(_, "NativeSelect", _) => additionalPropsFrom("Input")
        case Component(_, "Select", _)       => additionalPropsFrom("Input")

        //TODO from react-event-listener
        // case Component(_, "ClickAwayListener", _) => additionalPropsFrom("EventListener")

        //TODO from react-transition-group
        // case Component(_, "Collapse", _) => additionalPropsFrom("Transition")
        // case Component(_, "Fade", _) => additionalPropsFrom("Transition")
        // case Component(_, "Grow", _) => additionalPropsFrom("Transition")
        // case Component(_, "Slide", _) => additionalPropsFrom("Transition")
        // case Component(_, "Zoom", _) => additionalPropsFrom("Transition")

        case Component(_, "Dialog", _)  => additionalPropsFrom("Modal")
        case Component(_, "Popover", _) => additionalPropsFrom("Modal")

        case Component(_, "DialogContentText", _) => additionalPropsFrom("Typography")

        case Component(_, "RadioGroup", _) => additionalPropsFrom("FormGroup")

        case Component(_, "SwipeableDrawer", _) => additionalPropsFrom("Drawer")

        case Component(_, "TablePagination", _) => additionalPropsFrom("TableCell")

        case Component(_, "TextField", _) => additionalPropsFrom("FormControl")

        case Component(_, "CardContent", _) => additionalPropsFrom("DOCGEN_Children")

        case Component(_, _, props) => additionalPropsFrom()
      }
    }

    def useProp(c: Component, name: String, prop: Prop): Boolean =
      //TODO have a closer look at this - assuming that these are not meant to be used, but some of them look useful...
      //     In fact, it looks a lot like these are real properties that are passed through to underlying elements
      //     but that material-ui doesn't want to document (confusingly), and so we should use these properties.
      //     I think the only exception may be for children, where we probably want to treat this as not having children.
      // } else if (prop.required == false && prop.description.trim.toLowerCase == "@ignore") {
      //   false

      // Not sure what's up with this one - marked as ignore so get rid of it
      if (c.displayName == "Menu" && name == "theme") {
        false
      } else {
        true
      }

    val mouseEventNames: Set[String] = Set(
      "onClick",
      "onContextMenu",
      "onDoubleClick",
      "onDrag",
      "onDragEnd",
      "onDragEnter",
      "onDragExit",
      "onDragLeave",
      "onDragOver",
      "onDragStart",
      "onDrop",
      "onMouseDown",
      "onMouseEnter",
      "onMouseLeave",
      "onMouseMove",
      "onMouseOut",
      "onMouseOver",
      "onMouseUp"
    )

    val touchEventNames: Set[String] = Set(
      "onTouchCancel",
      "onTouchEnd",
      "onTouchMove",
      "onTouchStart"
    )

    val keyboardEventNames: Set[String] = Set(
      "onKeyDown",
      "onKeyPress",
      "onKeyUp"
    )

    val clipboardEventNames: Set[String] = Set(
      "onCopy",
      "onCut",
      "onPaste"
    )

    val compositionEventNames: Set[String] = Set(
      "onCompositionEnd",
      "onCompositionStart",
      "onCompositionUpdate"
    )

    def transformProp(c: Component, name: String, prop: Prop): (String, Prop) = {

      def namedFunc(s: String): Boolean        = name == s && prop.propType == FuncType
      def eventProp(s: String): (String, Prop) = name -> prop.copy(propType = eventFuncType(s))
      def funcInSet(s: Set[String]): Boolean   = s.contains(name) && prop.propType == FuncType

      if (c.displayName == "TextField" && name == "value") {
        name -> prop.copy(propType = StringType)

        // Style properties use mui.style.Style type
      } else if (name == "style" && prop.propType == ObjectType) {
        name -> prop.copy(propType = StyleType)

        //These two are only needed to add a description
      } else if (c.displayName == "TextField" && name == "onBlur") {
        name -> eventProp("ReactFocusEvent")._2
          .copy(description = "Passed to underlying input element")
      } else if (c.displayName == "TextField" && name == "onFocus") {
        name -> eventProp("ReactFocusEvent")._2
          .copy(description = "Passed to underlying input element")

        // Specific TextField events
      } else if (c.displayName == "TextField" && namedFunc("onChange")) {
        eventProp("ReactEventFromInput")

        // Change event
      } else if (namedFunc("onChange")) {
        eventProp("ReactEvent")

        // Type events according to
        // https://reactjs.org/docs/events.html and
        // https://github.com/japgolly/scalajs-react/blob/master/doc/TYPES.md#events

        //TODO Add a map from component names to underlying "EventSource" of the events,
        // e.g. TextField would have event source "Input" - if we have one of these, we will
        // add "FromSource" to the event type where appropriate, e.g. for ReactEventFromInput,
        // ReactFocusEventFromInput etc.

        // Focus Events
      } else if (namedFunc("onFocus")) {
        eventProp("ReactFocusEvent")
      } else if (namedFunc("onBlur")) {
        eventProp("ReactFocusEvent")
        // Clipboard Events
      } else if (funcInSet(clipboardEventNames)) {
        eventProp("ReactClipboardEvent")
        // Composition Events
      } else if (funcInSet(compositionEventNames)) {
        eventProp("ReactCompositionEvent")
        // Keyboard Events
      } else if (funcInSet(keyboardEventNames)) {
        eventProp("ReactKeyboardEvent")
        // Touch Events
      } else if (funcInSet(touchEventNames)) {
        eventProp("ReactTouchEvent")
        // Special case Touch Tap Event
      } else if (namedFunc("onTouchTap")) {
        eventProp("TouchTapEvent")
        // Mouse Events
      } else if (funcInSet(mouseEventNames)) {
        eventProp("ReactMouseEvent")
        // UI Events
      } else if (namedFunc("onScroll")) {
        eventProp("ReactUIEvent")
        // Wheel Events
      } else if (namedFunc("onWheel")) {
        eventProp("ReactWheelEvent")
        // General "on" handler func - ignore parameters
      } else if (name.startsWith("on") && prop.propType == FuncType) {
        name -> prop.copy(propType = callbackFuncType)

        // For some reason all the "classes" props are marked required, but they're not
      } else if (name == "classes" && prop.propType == ObjectType) {
        name -> prop.copy(required = false)

        // type is not a legal name in Scala
      } else if (name == "type") {
        "`type`" -> prop

      } else {
        name -> prop
      }
    }

  }

}
