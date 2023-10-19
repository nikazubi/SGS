import FormModal from "../../../components/modal/FormModal";
import useCreateSystemUserGroup from "./useCreateSystemUserGroup";
import useUpdateSystemUserGroup from "./useUpdateSystemUserGroup";
import SystemUserGroupForm from "./SystemUserGroupForm";
import {ModalOpenMode} from "../../../utils/constants";

const AcademyClassModal = ({open, groupId, groupName, groupStatus, subject, onClose, modalOpenMode, submitButton, ...props}) => {
    const {mutate: onCreate} = useCreateSystemUserGroup();
    const {mutate: onUpdate} = useUpdateSystemUserGroup();

    const initialValues = {
        id: groupId? groupId : 0,
        name: groupName? groupName : "",
        active: !!groupStatus,
        permission: subject? subject : ""
    };

    return (
        <FormModal
            open={open}
            title={"დამატება"}
            cancelText={'დახურვა'}
            saveText={'შენახვა'}
            width={700}
            height={350}
            maxWidth={700}
            maxHeight={350}
            initialValues={

                {
                    ...initialValues,
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={SystemUserGroupForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={modalOpenMode === ModalOpenMode.add ? onCreate : onUpdate}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default AcademyClassModal;