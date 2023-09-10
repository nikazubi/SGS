import FormModal from "../../../components/modal/FormModal";
import {useState} from "react";
import SystemUserForm from "./SystemUserForm";
import useCreateSystemuser from "./useCreateSystemuser";
import {ModalOpenMode} from "../../../utils/constants";
import useUpdateSystemUser from "./useUpdateSystemUser";


const SystemUserModal = ({data, open, news, onClose, modalOpenMode, submitButton, ...props}) => {

    const {mutate: onUpdate} = useUpdateSystemUser();
    const {mutate: onCreate} = useCreateSystemuser();

    const initialValues = {
        username: data?.username? data.username : '',
        academyClasses: data?.academyClasses? data.academyClasses : [],
        systemGroup: data?.systemGroup? data.systemGroup : [],
        name: data?.name? data.name : '',
        password: data?.password? data.password : '',
        email: data?.email? data.email : ''
    };

    return (
        <FormModal
            open={open}
            title={"დამატება"}
            cancelText={'დახურვა'}
            saveText={'შენახვა'}
            width={700}
            height={400}
            maxWidth={700}
            maxHeight={400}
            initialValues={

                {
                    ...initialValues
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={SystemUserForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={modalOpenMode === ModalOpenMode.add ? onCreate : onUpdate}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default SystemUserModal;