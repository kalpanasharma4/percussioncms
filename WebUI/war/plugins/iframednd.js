/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

(function($)
{
    $.perc_iframe_fix = function( frame )
    {
        //special scope for droppables which are inside the iframe, so that
        //they do not have spurious interactions with draggables outside the
        //iframe
        $.perc_iframe_scope = 'perc-iframe-scope';
        $.dragDelay = 10;
        var dragging = false;
        var iframeposTop = frame[0].offsetTop;
		var iframeposLeft = frame[0].offsetLeft;



        //Create an invisible div to put over the iframe
        var overlay = $("<div class=\"perc-iframe-overlay-dnd-container\"/>");
        $('body').append( overlay );

        overlay
            .height( frame[0].height )
            .width( frame[0].width )
            .addClass('ui-layout-ignore')
            .css(
                {
                    overflow: 'hidden',
                    position: 'absolute',
                    left: '-10000px',
                    top: '0px',
                    zIndex: 1000
                });

			var clicked = false;

			var curYPos = 0,curXPos = 0;
			overlay.on({
			'mousemove': function(e) {

				clicked && updateScrollPos(e);
			},
			'mousedown': function(e) {

				curYPos = e.clientY;
			}
		});

		var updateScrollPos = function(e) {
			curYPos =   iframeposTop;

			var x = iframeposLeft + ( e.clientX - curXPos);
			//var z = e.clientY;
			var y = (e.pageY - curYPos)
			frame[0].contentWindow.scrollTo(x,y );
		}

        addDragSupportDroppable();

        //Move the div over the iframe
        function addOverlay(event)
        {
            overlay.css({ left: frame[0].offsetLeft, top: frame[0].offsetTop  });
            overlay.height( frame[0].height  );
            overlay.width( frame[0].width );
            clicked = true;
			iframeposTop = frame[0].offsetTop;
		    iframeposLeft = frame[0].offsetLeft;
        }

        //Move the div back offscreen
        function removeOverlay()
        {
            overlay.css({ left: '-10000px', top: '0px' });
			clicked = false;
        }

        //Add droppable targets to the overlay div - this allows draggables outside the iframe to communicate with
        //droppables inside the iframe
        function liftDroppables( )
        {
            var droppables = frame.contents().find( ':data(ui-droppable)' );

            droppables.each( function()
            {
                var orig = $(this);
                var orig_drop = $.data( this, 'ui-droppable' );
                orig_drop.options.disabled=true;
                var clone = $("<div/>").addClass("allDroppablesHelpers").addClass("perc-iframe-dnd-overlay-droppable").attr("for", orig.attr("id")).width( orig.outerWidth() ).height( orig.outerHeight() );
                overlay.append( clone );
                var iframeLeft, iframeTop;
                var fr = frame;
                if( $.browser.msie )
                {
                    //Of *course* the scroll offsets would be in frame.contentWindow.document.documentElement - where else???
                    var contentWindow = fr[0].contentWindow;
                    var documentElement = contentWindow.document.documentElement;
                    iframeLeft = documentElement.scrollLeft;
                    iframeTop = documentElement.scrollTop;
                }
                else
                {
                    //Oh, you crazy other browsers, what a pathetically obvious place to put your scroll offsets!
                    iframeLeft = frame[0].contentWindow.scrollX;
                    iframeTop = frame[0].contentWindow.scrollY;
                }
                var left = $(this).offset().left - iframeLeft;
                var top = $(this).offset().top - iframeTop;
                if( $.browser.mozilla || $.browser.safari )
                {
                    //Fix offsets for scrolled window
                    left -= window.scrollX;
                    top -= window.scrollY;
                }
                clone.css( { position: 'absolute', left: left + "px", top: top + "px" } );

                //Make the clone droppable, with event functions which
                //call through to the original droppable's events
                clone.droppable(
                    {
                        greedy: orig_drop.options.greedy,
                        tolerance: orig_drop.options.tolerance,
                        accept: orig_drop.options.accept,
                        iframeFix: true,
                        scope: orig_drop.options.scope,
                        over: function(evt,ui){
                            evt.preventDefault();
                            orig_drop._over.call(orig_drop, [evt,ui]);
                        },
                        activate: function(evt,ui){
                            evt.preventDefault();
                            orig_drop._activate.call(orig_drop, [evt,ui]);
                        },
                        deactivate: function(evt,ui){
                            evt.preventDefault();
                            orig_drop._deactivate.call(orig_drop, [evt,ui]);
                        },
                        out: function(evt,ui){
                            evt.preventDefault();
                            orig_drop._out.call(orig_drop, [evt,ui]);
                        },
                        drop: function(evt,ui){
                            evt.preventDefault();
                            orig_drop._drop.call( orig_drop,[evt,ui]);
                        }
                    });
            });
        }

        //Get rid of the added droppables.
        function removeDroppables()
        {
            overlay.empty();
            var droppables = frame.contents().find( ':data(ui-droppable)' );

            droppables.each( function() {
                var orig = $(this);
                var orig_drop = $.data(this, 'ui-droppable');
                orig_drop.options.disabled = false;
            });
        }

        function onDragStart(event)
        {
            if( !dragging )
            {
                dragging = true;
                addOverlay(event);
                liftDroppables();
            }
        }

        function onDragStop()
        {
            removeOverlay();
            removeDroppables();
            dragging = false;
        }

        function addDragSupportDroppable()
        {
            var d = $("<div/>")
                .addClass('ui-layout-ignore')
                .droppable({
                    addClasses: false,
                    scope: $.perc_iframe_scope,
                    tolerance : 'pointer',
                    iframeFix: true,
                    activate: function(event,ui)
                    {
                        onDragStart(ui.draggable,event);
                    },
                    deactivate: function(event,ui)
                    {
                        setTimeout( onDragStop, 100 );
                    }
                });
            $('body').append(d);
        }

        //If a draggable needs to drag onto the iframe, it must call
        //startDrag() when dragging starts, end stopDrag() when dragging
        //stops

    };
})(jQuery);
