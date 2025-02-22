/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *  PercTooltip.js
 *  @author Jose Annunziato
 *
 *  Implements a simple tooltip
 *
 *  @usage
 *
 *  $(".element-with-a-title-attribute").PercToolTip()
 *
 *  Creates a singleton DOM with the following structure:
 *
 *  <pre>
 *      <div class="perc-tooltip perc-tooltip-arrow-down">
 *          <div class="perc-tooltip-content">
 *              The Content Of the Title Attribute
 *          </div>
 *          <div class="perc-tooltip-arrow-bottom perc-tooltip-arrow">
 *              <img src="/cm/widgets/PercTooltip/PercTooltipArrowDown.png">
 *          </div>
 *      </div>
 *  </pre>
 *
 *  which looks as shown below hovering over the text of the element the tooltip is applied to:
 *
 *  +------------------------------------+
 *  | The Content Of the Title Attribute |
 *  +----------------  ------------------+
 *                   \/
 *  This is the Text content of the element
 *
 *  An additional DIV, called the "hider", is applied on top of both the tooltip and the element
 *  to detect when the mouse leaves to hide the tooltip.
 *  This is because we dont want to hide the tooltip when the element looses focus.
 *  We want it to stick around so you can copy the content in the tooltip.
 */
(function($) {

    /**
     *  PercTooltip()
     *  Plugin implementation
     *
     */
    $.fn.PercTooltip = function(config) {
        $(this).
            on("mouseenter",function(e){
                $.PercTooltip.showTooltip(e);
        });
    };

    /**
     *  Singleton containing state variables, DOM, and methods implementing the tooltip behavior
     */
    $.PercTooltip = {
        tooltipDom : $("<div/>").
            addClass("perc-tooltip").
            addClass("perc-tooltip-arrow-down").
            append($("<div/>").addClass("perc-tooltip-content").append("Title Goes Here")).
            append($("<div/>").addClass("perc-tooltip-arrow-bottom perc-tooltip-arrow").append("<img src='/cm/widgets/PercTooltip/PercTooltipArrowDown.png'/>")).
            css("position","absolute").
            css("top","-10000px").
            css("left","-10000px").
            css("z-index","10000").
            on("mouseenter",function(event){$.PercTooltip.enterTooltip(event);}).
            on("mouseleave", function(event){$.PercTooltip.exitTooltip(event);}).
            on("click",function(event){event.stopPropagation();}),
        /*
         * this is the hider DIV that will be as big as the tooltip and element
         * to emulate blur on both the tooltip and element to then hide the tooltip
         */
        hiderDom : $("<div class='perc-tooltip-hider'/>").
            css("position","absolute").
            css("top","-10000px").
            css("left","-10000px").
            css("z-index","9000").
            on("mouseenter", function(event){$.PercTooltip.enterHider(event);}).
            on("mouseleave", function(event){$.PercTooltip.exitHider(event);}),

        /*
         *  State variables to keep track hover on tooltip and hider
         *  0 == not hovering on tooltip/hider
         *  1 == hovering on tooltip/hider
         *  TODO: make these named constants
         */
        hiderHoverState   : 0,
        tooltipHoverState : 0,
            
        showTooltip : function(event) {
            var element = $(event.currentTarget);
            
            var ePos    = element.position();
            var eLeft   = ePos.left;
            var eTop    = ePos.top;
            var eWidth  = element.width();
            var eHeight = element.height();
            
            var title = element.data("title");
            if(!title) {
                title = element.prop("title");
                element.data("title", title);
                element.prop("title",""); // clear the real title so we dont get the real tooltip
            }

            // dont bother with empty titles
            if(title.trim() === "")
                return;
            
            $.PercTooltip.tooltipDom.
                   find(".perc-tooltip-content").
                   html(title)
                   .css("display","block");
                   
               var tWidth  = $.PercTooltip.tooltipDom.width();
               var tHeight = $.PercTooltip.tooltipDom.height();
    
            var mLeft = event.pageX;
            var tLeft = mLeft - tWidth / 2;
            if(tLeft < 0)
                tLeft = 0;
            var tTop = eTop - tHeight + 1;  // TODO: make 7 into a named constant
            
            $.PercTooltip.tooltipDom.
                   find(".perc-tooltip-arrow").
                   css("position","absolute").
                   css("left", tWidth / 2);
            
            $.PercTooltip.tooltipDom.
                css("top",tTop).
                css("left",tLeft)
                .css("display","block");                

            $.PercTooltip.hiderDom.
                css("width",tWidth+5).
                css("height",eHeight + tHeight + 7).  // TODO: make 7 into a named constant
                css("top",tTop).
                css("left",eLeft).
                css({"background":"blue","opacity":0.0}).
                css("cursor",element.css("cursor")).
                off("click").on(function(event){
                	if(element.data("events") && element.data("events").click && element.data("events".click[0] && element.data("events").click[0].data && element.data("events").click[0].handler)){
	                    let e = {data : element.data("events").click[0].data};
	                    element.data("events").click[0].handler(e);
                	}
                });
        },
        exitHider : function(event) {
            this.hiderHoverState = 0;   // TODO: use a named constant instead
            var self = this;
            setTimeout(function(){self.hideTooltip(event);},500);
        },
        enterHider : function(event) {
            this.hiderHoverState = 1;   // TODO: use a named constant instead
            var self = this;
            setTimeout(function(){self.hideTooltip(event);},500);
        },
        exitTooltip : function(event) {
            this.tooltipHoverState = 0;   // TODO: use a named constant instead
            var self = this;
            setTimeout(function(){self.hideTooltip(event);},500);
        },
        enterTooltip : function(event) {
            this.tooltipHoverState = 1;   // TODO: use a named constant instead
            var self = this;
            setTimeout(function(){self.hideTooltip(event);},500);
        },
        hideTooltip : function(event) {
            // TODO: use named constants instead
            if(this.tooltipHoverState === 0 && this.hiderHoverState === 0) {
                this.tooltipDom.
                    blur().
                    css("top",-10000).
                    css("left",-10000).
                       find(".perc-tooltip-content").
                           html("");
                this.hiderDom.
                    css("top",-10000).
                    css("left",-10000);
            }
        }
    };

    $("body").append($.PercTooltip.tooltipDom)
    $("body").append($.PercTooltip.hiderDom);
    
})(jQuery);
