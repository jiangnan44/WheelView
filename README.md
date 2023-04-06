# WheelView

This WheelView is from here  [Android-PickerView](https://github.com/Bigkoo/Android-PickerView)
I just simplify it in some place.


## Effect Preview



## How to use

Just copy the source file WheelView and associate attrs to your project and fire off.

For demonstrate, I'v wrote two examples you might need:

PickWeekdayDialog (use DialogFragment, which maybe useful if you are so lucky that got some genius Chinese UI designers)
```kotlin

private var selectWeekday = "Wednesday"
private fun showWeekDialog(view: View?) {

    PickWeekdayDialog()
        .withInitDay(selectWeekday)
        .withWeekdayChooseListener { chosenDay ->
            selectWeekday = chosenDay
            Toast.makeText(this, "picked:$chosenDay", Toast.LENGTH_SHORT).show()
        }
        .show(supportFragmentManager)
}
```

And PickDateDialog (use BottomSheetDialogFragment, which you may have to solve scrolling problem if you have more than one scrolling child view)
```kotlin
    private var selectDate = "2023-5-12"
    private fun showDateDialog(view: View?) {

        PickDateDialog()
            .withYearRang(start = 1990, end = 2030)
            .withInitDate(selectDate)
            .withDateChooseListener { chosenDay ->
                selectDate = chosenDay
                Toast.makeText(this, "picked:$chosenDay", Toast.LENGTH_SHORT).show()
            }
            .show(supportFragmentManager)
    }
```

For custom UI, copy the resource file you needed, screw it the way you like.

Check the details in source code.


## Attributes

```xml
    <declare-styleable name="WheelView">
        <attr name="android:textSize" />
        <attr name="android:lineSpacingMultiplier" />

        <attr name="subTextColor" format="color" />
        <attr name="subTextGradient" format="boolean" />
        <attr name="centerTextColor" format="color" />
        <attr name="centerTextMidWeight" format="boolean" />
        <attr name="dividerColor" format="color" />
        <attr name="dividerVisible" format="boolean" />
        <attr name="canLoop" format="boolean" />
        <attr name="label" format="string" />
        <attr name="contentBias" format="float" />
        <attr name="labelBias" format="float" />
        <!--        should be odd-->
        <attr name="visibleItemCount" format="integer" />
    </declare-styleable>
```

textSize:the content text size, you can use setCenterTextTypeface() or setSubTextTypeface() to change typeface
lineSpacingMultiplier:use to adjust space between items
centerTextColor:the color of center text
centerTextMidWeight:true to make center text a fake middle weight,default is true
subTextColor:the color of text other than center text
subTextGradient:true to use alpha gradient effect for sub items
dividerColor:the color of center divider
dividerVisible:true to show center divider,default is false
canLoop:if those items can loop,default is false
label:a tag text attaches behind center text
contentBias:optional to adjust items position in horizontal direction,default is center
labelBias:optional to adjust label position in horizontal direction,default is align end
visibleItemCount:the total visible item count,should be odd


