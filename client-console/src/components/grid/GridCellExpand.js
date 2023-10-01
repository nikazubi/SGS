import {Avatar, Box, Tooltip} from "@mui/material";
import * as React from 'react';
import {useEffect, useRef, useState} from 'react';
import PropTypes from 'prop-types';
import {styled} from "@mui/material/styles";
import {resolveGridCellTextValue} from "./resolvers";
import ListToolTipTitle from "./ListToolTipTitle";

const CustomWidthTooltip = styled(({className, children, ...props}) => (
    <Tooltip {...props} classes={{popper: className}} children={children}/>
))(({width}) => ({
    maxWidth: width
}));

function isOverflown(element) {
    return (
        element.scrollHeight > element.clientHeight ||
        element.scrollWidth > element.clientWidth
    );
}

const GridCellExpand = React.memo(({value, width, valueProps, avatar, avatarProps, translate}) => {
    const cellValue = useRef(null);
    const [showToolTip, setShowToolTip] = useState(false);
    const resolvedValue = resolveGridCellTextValue(value, valueProps, translate);

    const resolveAvatar = () => {
        let alt;
        if (!!avatarProps?.alt) {
            alt = avatarProps.alt(resolvedValue);
        }
        return (
            <Avatar src={avatar}
                    variant={!!avatarProps?.variant ? avatarProps.variant : "circular"}
                    sx={{marginRight: 1}}
            >
                {alt?.toUpperCase() || resolvedValue?.charAt(0)?.toUpperCase()}
            </Avatar>
        );
    };

    const handleMouseEnter = () => {
        const isCurrentlyOverflown = isOverflown(cellValue.current);
        setShowToolTip(isCurrentlyOverflown);
    };

    const handleMouseLeave = () => {
        setShowToolTip(false);
    };

    const getTooltipTitle = () => {
        const title = !valueProps?.isList ? resolvedValue : ListToolTipTitle(value)
        return !!title ? title : "";
    }

    useEffect(() => {
        if (!showToolTip) {
            return undefined;
        }

        function handleKeyDown(nativeEvent) {
            if (nativeEvent.key === 'Escape' || nativeEvent.key === 'Esc') {
                setShowToolTip(false);
            }
        }

        document.addEventListener('keydown', handleKeyDown);

        return () => {
            document.removeEventListener('keydown', handleKeyDown);
        };
    }, [setShowToolTip, showToolTip]);

    return (
        <Box
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
            sx={{
                alignItems: 'center',
                lineHeight: '24px',
                width: 1,
                height: 1,
                position: 'relative',
                display: 'flex',
            }}
        >
            {avatar !== null && resolvedValue != null && (
                resolveAvatar()
            )}
            <CustomWidthTooltip
                title={getTooltipTitle()}
                width={width}
                open={showToolTip}
                interactive={true}
            >
                <Box
                    ref={cellValue}
                    sx={{whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}
                >
                    {resolvedValue}
                </Box>
            </CustomWidthTooltip>
        </Box>
    );
});

GridCellExpand.propTypes = {
    value: PropTypes.any.isRequired,
    width: PropTypes.number.isRequired,
};

const renderCellExpand = (params) => {
    return (
        <GridCellExpand
            value={params.value}
            width={params.colDef.computedWidth}
            valueProps={params.valueProps}
            avatarProps={params.avatarProps}
            translate={params.translate}
            avatar={params.avatar}
        />
    );
}

renderCellExpand.propTypes = {
    colDef: PropTypes.object.isRequired,
    value: PropTypes.any.isRequired,
};

export default renderCellExpand;